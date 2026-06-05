package com.exam.ai.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

/**
 * AI 文档级二阶段合并结果。
 */
@Schema(description = "AI 文档级合并结果")
@Builder
public record AiDocumentAssembleResult(
        @Schema(description = "合并后的完整题目列表")
        List<AiAssembledQuestionItem> questions
) {
}
