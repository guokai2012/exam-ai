package com.exam.ai.security;

import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.util.CurrentUserUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * ForcePasswordChangeFilter 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/me",
            "/api/auth/logout",
            "/api/auth/change-password"
    );

    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构造 ForcePasswordChangeFilter 实例并注入运行所需依赖。
     * @param userMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param objectMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public ForcePasswordChangeFilter(SysUserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param response 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param filterChain 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        UserPrincipal principal = CurrentUserUtils.getCurrentUser().orElse(null);
        if (principal != null) {
            SysUser user = userMapper.selectById(principal.userId());
            if (user != null && Boolean.TRUE.equals(user.getForcePasswordChange()) && !isAllowed(request.getRequestURI())) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(),
                        ApiResponse.error("PASSWORD_CHANGE_REQUIRED", "首次登录必须修改密码"));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param uri 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private boolean isAllowed(String uri) {
        return ALLOWED_PATHS.contains(uri);
    }
}

