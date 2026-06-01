package com.exam.ai.security;

import com.exam.ai.common.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * JwtService 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class JwtService {

    private final SecurityProperties properties;
    private final SecretKey secretKey;

    /**
     * 构造 JwtService 实例并注入运行所需依赖。
     * @param properties 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param username 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param sessionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param roles 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param permissions 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String createAccessToken(Long userId, String username, String sessionId, List<String> roles, List<String> permissions) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.getAccessTokenTtl());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("sessionId", sessionId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param token 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public JwtClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new JwtClaims(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("sessionId", String.class),
                stringList(claims.get("roles")),
                stringList(claims.get("permissions")),
                claims.getExpiration().toInstant()
        );
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param value 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
