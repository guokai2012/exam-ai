package com.exam.ai.system.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UpdateSystemConfigRequest 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "更新系统配置请求")
@Builder
public record UpdateSystemConfigRequest(
        @Schema(description = "配置值，最长 512 字符")
        @NotBlank
        @Size(max = 512)
        String configValue
) {
}

