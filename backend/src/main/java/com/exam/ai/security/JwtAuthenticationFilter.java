package com.exam.ai.security;

import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.util.CurrentUserUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JwtAuthenticationFilter 类，承载当前分层中的业务职责。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;

    /**
     * 构造 JwtAuthenticationFilter 实例并注入运行所需依赖。
     * @param jwtService 业务参数，参与当前方法的校验、查询或状态变更。
     * @param redisTemplate 业务参数，参与当前方法的校验、查询或状态变更。
     * @param userMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public JwtAuthenticationFilter(JwtService jwtService, StringRedisTemplate redisTemplate, SysUserMapper userMapper) {
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @param response 业务参数，参与当前方法的校验、查询或状态变更。
     * @param filterChain 业务参数，参与当前方法的校验、查询或状态变更。
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
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param token 业务参数，参与当前方法的校验、查询或状态变更。
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
            UserPrincipal principal = new UserPrincipal(
                    claims.userId(),
                    claims.username(),
                    claims.sessionId(),
                    claims.roles(),
                    claims.permissions()
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
     * @param request 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}

