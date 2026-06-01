package com.exam.ai.security;

import java.time.Instant;
import java.util.List;

/**
 * JwtClaims 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param username 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param sessionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param roles 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param permissions 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
 * @param expiresAt 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
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
