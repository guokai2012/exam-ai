package com.exam.ai.auth.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * TokenResponse 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "令牌响应")
@Builder
public record TokenResponse(
        @Schema(description = "访问令牌 JWT")
        String accessToken,
        @Schema(description = "刷新令牌")
        String refreshToken,
        @Schema(description = "令牌类型", example = "Bearer")
        String tokenType,
        @Schema(description = "访问令牌过期时间")
        Instant accessTokenExpiresAt
) {
}

