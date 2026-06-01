package com.exam.ai.user.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * PermissionScanResponse 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "权限扫描结果")
@Builder
public record PermissionScanResponse(
        @Schema(description = "新增权限数量")
        int created,
        @Schema(description = "更新权限数量")
        int updated,
        @Schema(description = "删除过期权限数量")
        int deleted
) {
}

