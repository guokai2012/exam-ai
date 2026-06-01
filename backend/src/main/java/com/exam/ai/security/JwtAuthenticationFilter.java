package com.exam.ai.security;

import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.entity.UserStatus;
import com.exam.ai.user.mapper.SysUserMapper;
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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final SysUserMapper userMapper;

    public JwtAuthenticationFilter(JwtService jwtService, StringRedisTemplate redisTemplate, SysUserMapper userMapper) {
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token != null) {
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

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
        } catch (JwtException | IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}

