package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * SaveRoleRequest 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "保存角色请求")
@Builder
public record SaveRoleRequest(
        @Schema(description = "角色编码", example = "TEACHER")
        @NotBlank @Size(max = 64) String roleCode,
        @Schema(description = "角色名称", example = "教师")
        @NotBlank @Size(max = 64) String roleName,
        @Schema(description = "权限码列表")
        @NotNull List<String> permissions
) {
}

