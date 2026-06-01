package com.exam.ai.security;

import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.mapper.SysUserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ForcePasswordChangeFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED_PATHS = Set.of(
            "/api/auth/me",
            "/api/auth/logout",
            "/api/auth/change-password"
    );

    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    public ForcePasswordChangeFilter(SysUserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
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

    private boolean isAllowed(String uri) {
        return ALLOWED_PATHS.contains(uri);
    }
}

