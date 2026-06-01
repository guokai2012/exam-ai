package com.exam.ai.document.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * AiQuestionItem 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "AI 识别题目项")
@Builder
public record AiQuestionItem(
        @Schema(description = "题型")
        String type,
        @Schema(description = "题干")
        String stem,
        @Schema(description = "选项列表")
        List<String> options,
        @Schema(description = "标准答案")
        String standardAnswer,
        @Schema(description = "解析")
        String explanation,
        @Schema(description = "难度星级")
        Integer difficultyStars,
        @Schema(description = "AI 置信度")
        BigDecimal confidence,
        @Schema(description = "分类名称")
        String categoryName
) {
}

