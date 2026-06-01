package com.exam.ai.system.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * SystemConfigResponse 记录对象，封装当前业务流程中的不可变数据。
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

