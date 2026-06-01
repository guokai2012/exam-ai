package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "重置密码请求")
@Builder
public record ResetPasswordRequest(
        @Schema(description = "新密码，6 到 64 位", example = "password123")
        @NotBlank @Size(min = 6, max = 64) String password
) {
}

