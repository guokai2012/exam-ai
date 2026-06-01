package com.exam.ai.auth.controller;

import com.exam.ai.auth.vo.CaptchaResponse;
import com.exam.ai.auth.dto.ChangePasswordRequest;
import com.exam.ai.auth.vo.CurrentUserResponse;
import com.exam.ai.auth.dto.LoginRequest;
import com.exam.ai.auth.dto.LogoutRequest;
import com.exam.ai.auth.dto.RefreshRequest;
import com.exam.ai.auth.dto.RegisterRequest;
import com.exam.ai.auth.vo.TokenResponse;
import com.exam.ai.auth.service.AuthService;
import com.exam.ai.auth.service.CaptchaService;
import com.exam.ai.auth.service.RateLimitService;
import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.common.config.SecurityProperties;
import com.exam.ai.security.ClientIp;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证接口控制器，负责验证码、注册、登录、刷新令牌、退出登录、改密和当前用户信息查询。
 *
 * <p>本类只处理 HTTP 参数、限流入口和 Cookie 写入，不承载账号校验、令牌轮换等业务逻辑，
 * 具体认证规则统一委托 {@link AuthService} 执行。</p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口", description = "验证码、注册、登录、刷新令牌、退出登录和当前用户信息")
public class AuthController {

    private static final String CAPTCHA_IP_LIMIT_NAME = "captcha-ip";
    private static final String CAPTCHA_IP_LIMIT_KEY = "captcha:ip";
    private static final int CAPTCHA_IP_DEFAULT_LIMIT = 20;
    private static final String LOGIN_IP_LIMIT_NAME = "login-ip";
    private static final String LOGIN_IP_LIMIT_KEY = "login:ip";
    private static final int LOGIN_IP_DEFAULT_LIMIT = 10;
    private static final String LOGIN_USER_LIMIT_NAME = "login-user";
    private static final String LOGIN_USER_LIMIT_KEY = "login:user";
    private static final int LOGIN_USER_DEFAULT_LIMIT = 5;
    private static final String REFRESH_IP_LIMIT_NAME = "refresh-ip";
    private static final String REFRESH_IP_LIMIT_KEY = "refresh:ip";
    private static final int REFRESH_IP_DEFAULT_LIMIT = 30;
    private static final Duration RATE_LIMIT_DEFAULT_WINDOW = Duration.ofMinutes(1);
    private static final String REFRESH_COOKIE_PATH = "/api/auth";
    private static final String REFRESH_COOKIE_SAME_SITE = "Strict";

    private final CaptchaService captchaService;
    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final SecurityProperties properties;

    /**
     * 构造认证接口控制器。
     *
     * @param captchaService 验证码生成与校验服务。
     * @param authService 账号注册、登录、令牌刷新和改密服务。
     * @param rateLimitService 登录、刷新和验证码接口的限流服务。
     * @param properties 认证相关配置，包括令牌有效期、Cookie 名称和限流窗口。
     */
    public AuthController(CaptchaService captchaService, AuthService authService,
                          RateLimitService rateLimitService, SecurityProperties properties) {
        this.captchaService = captchaService;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    /**
     * 生成登录验证码并返回验证码 ID 与图片。
     *
     * @param request 当前 HTTP 请求，用于解析客户端 IP 并执行 IP 级验证码限流。
     * @return 验证码 ID 和 Base64 图片内容。
     * @throws com.exam.ai.common.exception.BusinessException 当客户端触发验证码限流或 Redis 写入失败影响验证码创建时抛出。
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取验证码", description = "生成登录验证码图片，返回验证码 ID 和 Base64 图片内容。")
    @SecurityRequirements
    public ApiResponse<CaptchaResponse> captcha(HttpServletRequest request) {
        // 验证码接口按 IP 限流，防止匿名请求高频刷新验证码拖垮 Redis 或图片生成逻辑。
        rateLimitService.check(CAPTCHA_IP_LIMIT_KEY, ClientIp.resolve(request),
                properties.rateLimit(CAPTCHA_IP_LIMIT_NAME, CAPTCHA_IP_DEFAULT_LIMIT, RATE_LIMIT_DEFAULT_WINDOW));
        return ApiResponse.ok(captchaService.create());
    }

    /**
     * 注册学生账号。
     *
     * @param request 注册请求，包含用户名、密码和昵称。
     * @return 空响应，表示账号创建成功。
     * @throws com.exam.ai.common.exception.BusinessException 当用户名已存在、参数非法或默认学生角色不存在时抛出。
     */
    @PostMapping("/register")
    @Operation(summary = "注册账号", description = "注册学生账号，默认绑定学生角色。")
    @SecurityRequirements
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok();
    }

    /**
     * 账号登录并签发访问令牌与刷新令牌。
     *
     * @param request 登录请求，包含用户名、密码、验证码 ID 和验证码答案。
     * @param servletRequest 当前 HTTP 请求，用于获取客户端 IP 与 User-Agent。
     * @param servletResponse 当前 HTTP 响应，用于写入刷新令牌 Cookie。
     * @return 访问令牌、刷新令牌、令牌类型和访问令牌过期时间。
     * @throws com.exam.ai.common.exception.BusinessException 当触发限流、验证码错误、账号密码错误或账号被禁用时抛出。
     */
    @PostMapping("/login")
    @Operation(summary = "账号登录", description = "校验账号、密码和验证码，返回访问令牌并写入刷新令牌 Cookie。")
    @SecurityRequirements
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
                                            HttpServletResponse servletResponse) {
        String ip = ClientIp.resolve(servletRequest);
        // 登录同时按 IP 和用户名限流，分别覆盖撞库流量和单账号暴力破解风险。
        rateLimitService.check(LOGIN_IP_LIMIT_KEY, ip,
                properties.rateLimit(LOGIN_IP_LIMIT_NAME, LOGIN_IP_DEFAULT_LIMIT, RATE_LIMIT_DEFAULT_WINDOW));
        rateLimitService.check(LOGIN_USER_LIMIT_KEY, request.username(),
                properties.rateLimit(LOGIN_USER_LIMIT_NAME, LOGIN_USER_DEFAULT_LIMIT, RATE_LIMIT_DEFAULT_WINDOW));
        TokenResponse response = authService.login(request, ip, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        setRefreshCookie(servletResponse, response.refreshToken(), properties.getRefreshTokenTtl());
        return ApiResponse.ok(response);
    }

    /**
     * 使用刷新令牌轮换新的访问令牌和刷新令牌。
     *
     * @param request 可选请求体刷新令牌，前端本地存储 refresh token 时使用。
     * @param cookieToken 可选 Cookie 刷新令牌，浏览器自动携带时作为请求体缺省值。
     * @param servletRequest 当前 HTTP 请求，用于刷新限流和会话审计信息。
     * @param servletResponse 当前 HTTP 响应，用于写入轮换后的刷新令牌 Cookie。
     * @return 新的访问令牌和刷新令牌信息。
     * @throws com.exam.ai.common.exception.BusinessException 当刷新令牌缺失、过期、复用、撤销或触发限流时抛出。
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用请求体或 Cookie 中的 refresh token 换取新的访问令牌。")
    @SecurityRequirements
    public ApiResponse<TokenResponse> refresh(@RequestBody(required = false) RefreshRequest request,
                                              @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String cookieToken,
                                              HttpServletRequest servletRequest,
                                              HttpServletResponse servletResponse) {
        String ip = ClientIp.resolve(servletRequest);
        // 刷新接口允许静默调用，但仍按 IP 限流以避免失效令牌被高频探测。
        rateLimitService.check(REFRESH_IP_LIMIT_KEY, ip,
                properties.rateLimit(REFRESH_IP_LIMIT_NAME, REFRESH_IP_DEFAULT_LIMIT, RATE_LIMIT_DEFAULT_WINDOW));
        String refreshToken = request != null && request.refreshToken() != null ? request.refreshToken() : cookieToken;
        TokenResponse response = authService.refresh(refreshToken, ip, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        setRefreshCookie(servletResponse, response.refreshToken(), properties.getRefreshTokenTtl());
        return ApiResponse.ok(response);
    }

    /**
     * 退出登录并撤销当前刷新令牌。
     *
     * @param request 可选请求体刷新令牌，兼容前端主动传入的 refresh token。
     * @param cookieToken 可选 Cookie 刷新令牌，作为请求体缺省值。
     * @param servletResponse 当前 HTTP 响应，用于清空刷新令牌 Cookie。
     * @return 空响应，表示退出流程已完成。
     * @throws com.exam.ai.common.exception.BusinessException 当登录用户无退出权限时抛出。
     */
    @PostMapping("/logout")
    @PreAuthorize("hasAuthority('auth:logout')")
    @Operation(summary = "退出登录", description = "撤销当前 refresh token 并清空刷新令牌 Cookie。")
    public ApiResponse<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                    @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String cookieToken,
                                    HttpServletResponse servletResponse) {
        String refreshToken = request != null && request.refreshToken() != null ? request.refreshToken() : cookieToken;
        authService.logout(refreshToken);
        // 服务端撤销后同步清空浏览器 Cookie，避免前端再次携带已失效刷新令牌。
        setRefreshCookie(servletResponse, "", Duration.ZERO);
        return ApiResponse.ok();
    }

    /**
     * 当前登录用户修改密码。
     *
     * @param request 改密请求，包含原密码和新密码。
     * @return 空响应，表示密码已更新。
     * @throws com.exam.ai.common.exception.BusinessException 当原密码错误、新密码不合法或用户不存在时抛出。
     */
    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('password:change')")
    @Operation(summary = "修改密码", description = "登录用户修改密码，成功后更新密码状态。")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.ok();
    }

    /**
     * 查询当前登录用户信息。
     *
     * @return 当前用户基础资料、角色、权限以及是否需要强制修改密码。
     * @throws com.exam.ai.common.exception.BusinessException 当登录上下文缺失或用户记录不存在时抛出。
     */
    @GetMapping("/me")
    @PreAuthorize("hasAuthority('auth:me')")
    @Operation(summary = "当前用户信息", description = "返回当前登录用户的基础资料、角色、权限和是否强制改密。")
    public ApiResponse<CurrentUserResponse> me() {
        return ApiResponse.ok(authService.currentUser());
    }

    /**
     * 写入刷新令牌 Cookie。
     *
     * @param response 当前 HTTP 响应。
     * @param refreshToken 要写入浏览器的刷新令牌，传入空字符串时用于清空 Cookie。
     * @param maxAge Cookie 生命周期，退出登录时传入 {@link Duration#ZERO}。
     */
    private void setRefreshCookie(HttpServletResponse response, String refreshToken, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(properties.getRefreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite(REFRESH_COOKIE_SAME_SITE)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
