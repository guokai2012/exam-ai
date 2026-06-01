package com.exam.ai.auth.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * LogoutRequest 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "退出登录请求")
@Builder
public record LogoutRequest(
        @Schema(description = "刷新令牌；不传时使用 Cookie 中的刷新令牌")
        String refreshToken
) {
}

