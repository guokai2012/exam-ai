package com.exam.ai.user.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * PermissionResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "权限树节点")
@Builder
public record PermissionResponse(
        @Schema(description = "权限 ID")
        Long id,
        @Schema(description = "父权限 ID")
        Long parentId,
        @Schema(description = "关联菜单 ID")
        Long menuId,
        @Schema(description = "权限码")
        String permissionCode,
        @Schema(description = "权限名称")
        String permissionName,
        @Schema(description = "权限类型：GROUP 分组，MENU 菜单，VIEW 查看，ACTION 动作")
        String permissionType,
        @Schema(description = "排序值")
        Integer sortOrder,
        @Schema(description = "是否系统生成")
        Boolean systemGenerated,
        @Schema(description = "是否可分配给角色")
        Boolean assignable,
        @Schema(description = "子权限")
        List<PermissionResponse> children
) {
}

