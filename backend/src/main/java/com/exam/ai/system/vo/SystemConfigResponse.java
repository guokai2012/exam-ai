package com.exam.ai.system.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * SystemConfigResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "系统配置响应")
@Builder
public record SystemConfigResponse(
        @Schema(description = "配置键")
        String configKey,
        @Schema(description = "配置值")
        String configValue,
        @Schema(description = "配置名称")
        String configName,
        @Schema(description = "配置说明")
        String description,
        @Schema(description = "值类型")
        String valueType,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}

