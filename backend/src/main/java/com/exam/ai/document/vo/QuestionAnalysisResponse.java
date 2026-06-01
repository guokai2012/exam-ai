package com.exam.ai.document.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

/**
 * QuestionAnalysisResponse 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "文档分析题目响应")
@Builder
public record QuestionAnalysisResponse(
        @Schema(description = "题目 ID")
        Long questionId,
        @Schema(description = "分类 ID")
        Long categoryId,
        @Schema(description = "分类名称")
        String categoryName,
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
        @Schema(description = "题目状态")
        String state,
        @Schema(description = "是否本次新建")
        Boolean newlyCreated,
        @Schema(description = "AI 置信度")
        BigDecimal confidence,
        @Schema(description = "排序值")
        Integer sortOrder
) {
}

