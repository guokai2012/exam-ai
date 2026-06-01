package com.exam.ai.auth.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CaptchaResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "验证码响应")
@Builder
public record CaptchaResponse(
        @Schema(description = "验证码 ID", example = "f2a7c8")
        String captchaId,
        @Schema(description = "验证码图片 Base64")
        String imageBase64
) {
}

