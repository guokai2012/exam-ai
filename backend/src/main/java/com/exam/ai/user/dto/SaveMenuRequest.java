package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "保存菜单请求")
@Builder
public record SaveMenuRequest(
        @Schema(description = "父菜单 ID，根菜单为空")
        Long parentId,
        @Schema(description = "菜单名称")
        @NotBlank @Size(max = 64) String menuName,
        @Schema(description = "前端路由路径")
        @NotBlank @Size(max = 128) String path,
        @Schema(description = "前端组件标识")
        @NotBlank @Size(max = 128) String component,
        @Schema(description = "图标名称")
        @Size(max = 64) String icon,
        @Schema(description = "排序值")
        @NotNull Integer sortOrder,
        @Schema(description = "状态：1 启用，0 禁用")
        @NotNull Integer status,
        @Schema(description = "访问菜单所需权限码，留空时普通菜单自动生成查看权限码")
        @Size(max = 128) String permissionCode
) {
}

