package com.exam.ai.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 前端路由扫描得到的菜单节点同步请求。
 *
 * @param menuKey 前端生成的稳定菜单标识，分组菜单依赖该字段与数据库记录建立长期关联。
 * @param menuName 菜单展示名称。
 * @param path 前端页面路径；为空表示分组菜单。
 * @param apiPath 页面主资源 API 根路径；分组菜单会被后端强制清空。
 * @param icon Element Plus 图标名称。
 * @param sortOrder 菜单排序值。
 * @param status 菜单状态，1 表示启用，0 表示禁用。
 * @param permissionCode 访问菜单所需权限码；分组菜单会被后端强制清空。
 * @param children 子菜单节点。
 */
@Schema(description = "扫描菜单节点请求")
public record SyncMenuItemRequest(
        @Schema(description = "稳定菜单标识")
        @NotBlank @Size(max = 128) String menuKey,
        @Schema(description = "菜单名称")
        @NotBlank @Size(max = 64) String menuName,
        @Schema(description = "前端路由路径；为空表示分组菜单")
        @Size(max = 128) String path,
        @Schema(description = "页面主资源 API 根路径")
        @Size(max = 128) String apiPath,
        @Schema(description = "图标名称")
        @Size(max = 64) String icon,
        @Schema(description = "排序值")
        @NotNull Integer sortOrder,
        @Schema(description = "状态：1 启用，0 禁用")
        @NotNull Integer status,
        @Schema(description = "访问菜单所需权限码")
        @Size(max = 128) String permissionCode,
        @Schema(description = "子菜单")
        List<@Valid SyncMenuItemRequest> children
) {
}
