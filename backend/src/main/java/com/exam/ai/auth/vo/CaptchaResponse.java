package com.exam.ai.auth.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * CaptchaResponse 记录对象，封装当前业务流程中的不可变数据。
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

