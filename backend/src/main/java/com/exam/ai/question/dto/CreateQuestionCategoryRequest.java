package com.exam.ai.question.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CreateQuestionCategoryRequest 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "创建题目分类请求")
@Builder
public record CreateQuestionCategoryRequest(
        @Schema(description = "分类名称", example = "高等数学")
        @NotBlank @Size(max = 128) String categoryName,
        @Schema(description = "分类描述")
        @Size(max = 512) String description
) {
}

