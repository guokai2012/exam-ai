package com.exam.ai.document.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DocumentContentResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "文档解析文本响应")
@Builder
public record DocumentContentResponse(
        @Schema(description = "文档 ID")
        Long documentId,
        @Schema(description = "解析出的文本")
        String extractedText
) {
}

