package com.exam.ai.system.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * SystemConfigUpdateResult 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
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

