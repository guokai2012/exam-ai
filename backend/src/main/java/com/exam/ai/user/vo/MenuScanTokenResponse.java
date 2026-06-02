package com.exam.ai.user.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * 菜单扫描临时 Token 响应。
 *
 * @param token 菜单扫描专用临时 Token，仅允许前端保存在内存中。
 * @param expiresAt 临时 Token 过期时间。
 */
@Schema(description = "菜单扫描临时 Token 响应")
public record MenuScanTokenResponse(
        @Schema(description = "菜单扫描临时 Token") String token,
        @Schema(description = "过期时间") Instant expiresAt
) {
}
