package com.exam.ai.question.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ReviewQuestionRequest 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "题目审核请求")
@Builder
public record ReviewQuestionRequest(
        @Schema(description = "是否审核通过")
        @NotNull Boolean approved,
        @Schema(description = "通过时可指定分类 ID")
        Long categoryId,
        @Schema(description = "驳回或调整原因")
        @Size(max = 512) String reason
) {
}

