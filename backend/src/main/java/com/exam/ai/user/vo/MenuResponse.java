package com.exam.ai.user.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * MenuResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "菜单树节点")
@Builder
public record MenuResponse(
        @Schema(description = "菜单 ID")
        Long id,
        @Schema(description = "父菜单 ID")
        Long parentId,
        @Schema(description = "菜单名称")
        String menuName,
        @Schema(description = "前端路由路径")
        String path,
        @Schema(description = "前端组件标识")
        String component,
        @Schema(description = "图标名称")
        String icon,
        @Schema(description = "排序值")
        Integer sortOrder,
        @Schema(description = "状态：1 启用，0 禁用")
        Integer status,
        @Schema(description = "访问菜单所需权限码")
        String permissionCode,
        @Schema(description = "子菜单")
        List<MenuResponse> children
) {
}

