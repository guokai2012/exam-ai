package com.exam.ai.document.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文档解析文本响应")
@Builder
public record DocumentContentResponse(
        @Schema(description = "文档 ID")
        Long documentId,
        @Schema(description = "解析出的文本")
        String extractedText
) {
}

