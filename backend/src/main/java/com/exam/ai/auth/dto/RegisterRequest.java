package com.exam.ai.auth.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "注册请求")
@Builder
public record RegisterRequest(
        @Schema(description = "用户名，3 到 64 位", example = "student01")
        @NotBlank @Size(min = 3, max = 64) String username,
        @Schema(description = "密码，6 到 64 位", example = "password123")
        @NotBlank @Size(min = 6, max = 64) String password,
        @Schema(description = "昵称", example = "学生一号")
        @NotBlank @Size(max = 64) String nickname
) {
}

