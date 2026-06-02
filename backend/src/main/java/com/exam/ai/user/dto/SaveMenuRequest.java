package com.exam.ai.user.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * SaveMenuRequest 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "保存菜单请求")
@Builder
public record SaveMenuRequest(
        @Schema(description = "菜单名称")
        @NotBlank @Size(max = 64) String menuName,
        @Schema(description = "图标名称")
        @Size(max = 64) String icon,
        @Schema(description = "排序值")
        @NotNull Integer sortOrder,
        @Schema(description = "状态：1 启用，0 禁用")
        @NotNull Integer status,
        @Schema(description = "页面主资源 API 根路径；分组菜单必须为空")
        @Size(max = 128) String apiPath
) {
}

