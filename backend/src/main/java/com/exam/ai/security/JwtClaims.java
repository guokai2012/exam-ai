package com.exam.ai.security;

import java.time.Instant;
import java.util.List;

public record JwtClaims(
        Long userId,
        String username,
        String sessionId,
        List<String> roles,
        List<String> permissions,
        Instant expiresAt
) {
}
