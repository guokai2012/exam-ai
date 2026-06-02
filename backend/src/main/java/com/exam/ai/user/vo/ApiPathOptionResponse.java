package com.exam.ai.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API 路径下拉选项，供菜单管理页面绑定页面主资源接口根路径。
 *
 * @param label 展示名称，优先取 Controller 的 {@code @Tag.name}
 * @param value API 根路径，例如 {@code /api/documents}
 */
@Schema(description = "菜单可绑定 API 路径选项")
public record ApiPathOptionResponse(
        @Schema(description = "API 中文名称")
        String label,
        @Schema(description = "API 根路径")
        String value
) {
}
