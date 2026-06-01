package com.exam.ai.auth.controller;

import com.exam.ai.auth.dto.CaptchaResponse;
import com.exam.ai.auth.dto.ChangePasswordRequest;
import com.exam.ai.auth.dto.CurrentUserResponse;
import com.exam.ai.auth.dto.LoginRequest;
import com.exam.ai.auth.dto.LogoutRequest;
import com.exam.ai.auth.dto.RefreshRequest;
import com.exam.ai.auth.dto.RegisterRequest;
import com.exam.ai.auth.dto.TokenResponse;
import com.exam.ai.auth.service.AuthService;
import com.exam.ai.auth.service.CaptchaService;
import com.exam.ai.auth.service.RateLimitService;
import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.config.SecurityProperties;
import com.exam.ai.security.ClientIp;
import com.exam.ai.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证接口", description = "验证码、注册、登录、刷新令牌、退出登录和当前用户信息")
public class AuthController {

    private final CaptchaService captchaService;
    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final SecurityProperties properties;

    public AuthController(CaptchaService captchaService, AuthService authService,
                          RateLimitService rateLimitService, SecurityProperties properties) {
        this.captchaService = captchaService;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
        this.properties = properties;
    }

    @GetMapping("/captcha")
    @Operation(summary = "获取验证码", description = "生成登录验证码图片，返回验证码 ID 和 Base64 图片内容。")
    @SecurityRequirements
    public ApiResponse<CaptchaResponse> captcha(HttpServletRequest request) {
        rateLimitService.check("captcha:ip", ClientIp.resolve(request), properties.rateLimit("captcha-ip", 20, Duration.ofMinutes(1)));
        return ApiResponse.ok(captchaService.create());
    }

    @PostMapping("/register")
    @Operation(summary = "注册账号", description = "注册学生账号，默认绑定学生角色。")
    @SecurityRequirements
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.ok();
    }

    @PostMapping("/login")
    @Operation(summary = "账号登录", description = "校验账号、密码和验证码，返回访问令牌并写入刷新令牌 Cookie。")
    @SecurityRequirements
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
                                            HttpServletResponse servletResponse) {
        String ip = ClientIp.resolve(servletRequest);
        rateLimitService.check("login:ip", ip, properties.rateLimit("login-ip", 10, Duration.ofMinutes(1)));
        rateLimitService.check("login:user", request.username(), properties.rateLimit("login-user", 5, Duration.ofMinutes(1)));
        TokenResponse response = authService.login(request, ip, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        setRefreshCookie(servletResponse, response.refreshToken(), properties.getRefreshTokenTtl());
        return ApiResponse.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用请求体或 Cookie 中的 refresh token 换取新的访问令牌。")
    @SecurityRequirements
    public ApiResponse<TokenResponse> refresh(@RequestBody(required = false) RefreshRequest request,
                                              @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String cookieToken,
                                              HttpServletRequest servletRequest,
                                              HttpServletResponse servletResponse) {
        String ip = ClientIp.resolve(servletRequest);
        rateLimitService.check("refresh:ip", ip, properties.rateLimit("refresh-ip", 30, Duration.ofMinutes(1)));
        String refreshToken = request != null && request.refreshToken() != null ? request.refreshToken() : cookieToken;
        TokenResponse response = authService.refresh(refreshToken, ip, servletRequest.getHeader(HttpHeaders.USER_AGENT));
        setRefreshCookie(servletResponse, response.refreshToken(), properties.getRefreshTokenTtl());
        return ApiResponse.ok(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAuthority('auth:logout')")
    @Operation(summary = "退出登录", description = "撤销当前 refresh token 并清空刷新令牌 Cookie。")
    public ApiResponse<Void> logout(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                    @RequestBody(required = false) LogoutRequest request,
                                    @CookieValue(name = "${app.security.refresh-cookie-name}", required = false) String cookieToken,
                                    HttpServletResponse servletResponse) {
        String refreshToken = request != null && request.refreshToken() != null ? request.refreshToken() : cookieToken;
        authService.logout(principal, refreshToken);
        setRefreshCookie(servletResponse, "", Duration.ZERO);
        return ApiResponse.ok();
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasAuthority('password:change')")
    @Operation(summary = "修改密码", description = "登录用户修改密码，成功后更新密码状态。")
    public ApiResponse<Void> changePassword(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal, request);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('auth:me')")
    @Operation(summary = "当前用户信息", description = "返回当前登录用户的基础资料、角色、权限和是否强制改密。")
    public ApiResponse<CurrentUserResponse> me(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(authService.currentUser(principal));
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(properties.getRefreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(properties.isRefreshCookieSecure())
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
