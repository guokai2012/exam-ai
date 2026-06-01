package com.exam.ai.auth.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ChangePasswordRequest 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "修改密码请求")
@Builder
public record ChangePasswordRequest(
        @Schema(description = "原密码", example = "oldPassword123")
        @NotBlank String oldPassword,
        @Schema(description = "新密码，6 到 64 位", example = "newPassword123")
        @NotBlank @Size(min = 6, max = 64) String newPassword
) {
}

