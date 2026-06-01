package com.exam.ai.question.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * QuestionCategoryResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
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

