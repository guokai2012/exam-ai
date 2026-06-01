package com.exam.ai.security;

import java.time.Instant;
import java.util.List;

/**
 * JwtClaims 记录对象，封装当前业务流程中的不可变数据。
 * @param userId 业务参数，参与当前方法的校验、查询或状态变更。
 * @param username 业务参数，参与当前方法的校验、查询或状态变更。
 * @param sessionId 业务参数，参与当前方法的校验、查询或状态变更。
 * @param roles 业务参数，参与当前方法的校验、查询或状态变更。
 * @param permissions 业务参数，参与当前方法的校验、查询或状态变更。
 * @param expiresAt 业务参数，参与当前方法的校验、查询或状态变更。
 */
public record JwtClaims(
        Long userId,
        String username,
        String sessionId,
        List<String> roles,
        List<String> permissions,
        Instant expiresAt
) {
}
