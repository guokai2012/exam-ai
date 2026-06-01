package com.exam.ai.question.dto;

import lombok.Builder;

import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.question.entity.ExamQuestionBank;
import com.exam.ai.question.entity.ExamQuestionCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * QuestionImportResult 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "题目导入结果")
@Builder
public record QuestionImportResult(
        @Schema(description = "题目实体")
        ExamQuestionBank question,
        @Schema(description = "分类实体")
        ExamQuestionCategory category,
        @Schema(description = "AI 题目项")
        AiQuestionItem item,
        @Schema(description = "AI 置信度")
        BigDecimal confidence,
        @Schema(description = "排序值")
        int sortOrder,
        @Schema(description = "是否新建题目")
        boolean newlyCreated
) {
}


