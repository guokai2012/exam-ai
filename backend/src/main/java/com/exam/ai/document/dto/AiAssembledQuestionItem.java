package com.exam.ai.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;

/**
 * AI 二阶段合并后的完整题目项。
 */
@Schema(description = "AI 合并后的完整题目项")
@Builder
public record AiAssembledQuestionItem(
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
        String categoryName,
        @Schema(description = "来源页码")
        List<Integer> sourcePageNos
) {
}
