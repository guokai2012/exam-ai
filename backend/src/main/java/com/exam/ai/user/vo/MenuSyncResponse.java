package com.exam.ai.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜单扫描同步结果。
 *
 * @param created 新增菜单数量。
 * @param updated 补齐字段的菜单数量。
 * @param skipped 已存在且无需补齐的菜单数量。
 */
@Schema(description = "菜单扫描同步结果")
public record MenuSyncResponse(
        @Schema(description = "新增数量") int created,
        @Schema(description = "更新数量") int updated,
        @Schema(description = "跳过数量") int skipped
) {
}
