package com.exam.ai.user.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * RoleResponse 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "角色响应")
@Builder
public record RoleResponse(
        @Schema(description = "角色 ID")
        Long id,
        @Schema(description = "角色编码")
        String roleCode,
        @Schema(description = "角色名称")
        String roleName,
        @Schema(description = "权限码列表")
        List<String> permissions
) {
}

