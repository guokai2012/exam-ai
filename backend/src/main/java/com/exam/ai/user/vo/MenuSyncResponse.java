package com.exam.ai.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 菜单扫描同步结果。
 *
 * @param created 新增菜单数量。
 * @param updated 覆盖更新菜单数量。
 * @param deleted 扫描结果中不存在并被逻辑删除的菜单数量。
 */
@Schema(description = "菜单扫描同步结果")
public record MenuSyncResponse(
        @Schema(description = "新增数量") int created,
        @Schema(description = "更新数量") int updated,
        @Schema(description = "删除数量") int deleted
) {
}
