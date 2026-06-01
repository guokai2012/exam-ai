package com.exam.ai.document.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * AiQuestionResult 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "AI 识别题目结果")
@Builder
public record AiQuestionResult(
        @Schema(description = "题目列表")
        List<AiQuestionItem> questions
) {
}

