package com.exam.ai.system.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "系统配置更新结果")
@Builder
public record SystemConfigUpdateResult(
        @Schema(description = "更新后的配置")
        SystemConfigResponse config,
        @Schema(description = "更新提示消息")
        String message
) {
}

