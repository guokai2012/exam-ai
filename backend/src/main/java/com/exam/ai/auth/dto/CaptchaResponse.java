package com.exam.ai.auth.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "验证码响应")
@Builder
public record CaptchaResponse(
        @Schema(description = "验证码 ID", example = "f2a7c8")
        String captchaId,
        @Schema(description = "验证码图片 Base64")
        String imageBase64
) {
}

