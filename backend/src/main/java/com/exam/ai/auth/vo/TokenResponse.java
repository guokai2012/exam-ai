package com.exam.ai.auth.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * TokenResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
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

