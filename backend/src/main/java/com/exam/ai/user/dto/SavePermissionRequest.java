package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SavePermissionRequest 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "保存权限请求")
@Builder
public record SavePermissionRequest(
        @Schema(description = "父权限 ID，留空时归入未归类权限")
        Long parentId,
        @Schema(description = "权限码")
        @NotBlank @Size(max = 128) String permissionCode,
        @Schema(description = "权限名称")
        @NotBlank @Size(max = 128) String permissionName,
        @Schema(description = "排序值")
        Integer sortOrder
) {
}

