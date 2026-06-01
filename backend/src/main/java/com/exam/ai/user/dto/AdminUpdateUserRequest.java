package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * AdminUpdateUserRequest 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "后台编辑用户请求")
@Builder
public record AdminUpdateUserRequest(
        @Schema(description = "昵称")
        @NotBlank @Size(max = 64) String nickname,
        @Schema(description = "用户状态：1 启用，0 禁用", example = "1")
        @NotNull Integer status,
        @Schema(description = "角色编码列表")
        @NotEmpty List<String> roles
) {
}

