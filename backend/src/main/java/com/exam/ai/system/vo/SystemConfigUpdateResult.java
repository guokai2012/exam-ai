package com.exam.ai.system.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SystemConfigUpdateResult 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "系统配置更新结果")
@Builder
public record SystemConfigUpdateResult(
        @Schema(description = "更新后的配置")
        SystemConfigResponse config,
        @Schema(description = "更新提示消息")
        String message
) {
}

