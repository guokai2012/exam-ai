package com.exam.ai.system.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

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

