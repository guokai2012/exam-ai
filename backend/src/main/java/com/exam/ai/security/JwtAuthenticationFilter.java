package com.exam.ai.security;

import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.user.service.RolePermissionService;
import com.exam.ai.util.CurrentUserUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JwtAuthenticationFilter 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;
    private final RolePermissionService rolePermissionService;

    /**
     * 构造 JwtAuthenticationFilter 实例并注入运行所需依赖。
     * @param jwtService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param redisTemplate 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param rolePermissionService 用户角色权限服务，用于按用户 ID 读取数据库实时授权快照。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public JwtAuthenticationFilter(JwtService jwtService, StringRedisTemplate redisTemplate, SysUserMapper userMapper,
                                   RolePermissionService rolePermissionService) {
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
        this.rolePermissionService = rolePermissionService;
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
        try {
            CurrentUserUtils.clear();
            String token = resolveBearerToken(request);
            if (token != null) {
                authenticate(token);
            }
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserUtils.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param token 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void authenticate(String token) {
        try {
            JwtClaims claims = jwtService.parse(token);
            String currentSession = redisTemplate.opsForValue().get(RedisKeys.session(claims.userId()));
            if (!claims.sessionId().equals(currentSession)) {
                return;
            }
            SysUser user = userMapper.selectById(claims.userId());
            if (user == null || !Integer.valueOf(UserStatus.ENABLED).equals(user.getStatus())) {
                return;
            }
            // 角色和权限可能在用户登录后被管理员调整，认证上下文必须以数据库实时授权为准。
            List<String> roles = rolePermissionService.roles(claims.userId());
            List<String> permissions = rolePermissionService.permissions(claims.userId());
            UserPrincipal principal = new UserPrincipal(
                    claims.userId(),
                    claims.username(),
                    claims.sessionId(),
                    roles,
                    permissions
            );
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            CurrentUserUtils.setCurrentUser(principal);
        } catch (JwtException | IllegalArgumentException ignored) {
            CurrentUserUtils.clear();
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}

