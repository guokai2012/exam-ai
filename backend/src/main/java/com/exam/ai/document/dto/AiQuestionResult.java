package com.exam.ai.document.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "AI 识别题目结果")
@Builder
public record AiQuestionResult(
        @Schema(description = "题目列表")
        List<AiQuestionItem> questions
) {
}

