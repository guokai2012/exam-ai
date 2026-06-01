package com.exam.ai.auth.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * LoginRequest 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "登录请求")
@Builder
public record LoginRequest(
        @Schema(description = "用户名", example = "admin")
        @NotBlank String username,
        @Schema(description = "密码", example = "admin123")
        @NotBlank String password,
        @Schema(description = "验证码 ID")
        @NotBlank String captchaId,
        @Schema(description = "验证码内容", example = "8K2P")
        @NotBlank String captchaCode
) {
}

