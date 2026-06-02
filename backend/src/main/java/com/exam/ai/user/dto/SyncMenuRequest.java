package com.exam.ai.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 前端路由扫描菜单同步请求。
 *
 * @param menus 前端扫描得到的菜单树。
 */
@Schema(description = "扫描菜单同步请求")
public record SyncMenuRequest(
        @Schema(description = "菜单树")
        @NotEmpty List<@Valid SyncMenuItemRequest> menus
) {
}
