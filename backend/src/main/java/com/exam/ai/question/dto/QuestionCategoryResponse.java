package com.exam.ai.question.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目分类响应")
@Builder
public record QuestionCategoryResponse(
        @Schema(description = "分类 ID")
        Long id,
        @Schema(description = "分类名称")
        String categoryName,
        @Schema(description = "分类描述")
        String description,
        @Schema(description = "状态：1 启用，0 禁用")
        Integer status
) {
}

