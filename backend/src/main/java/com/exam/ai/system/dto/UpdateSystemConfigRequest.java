package com.exam.ai.system.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UpdateSystemConfigRequest 记录对象，封装当前业务流程中的不可变数据。
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

