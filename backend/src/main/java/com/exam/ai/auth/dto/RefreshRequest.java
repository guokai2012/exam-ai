package com.exam.ai.auth.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * RefreshRequest 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "刷新令牌请求")
@Builder
public record RefreshRequest(
        @Schema(description = "刷新令牌；不传时使用 Cookie 中的刷新令牌")
        String refreshToken
) {
}

