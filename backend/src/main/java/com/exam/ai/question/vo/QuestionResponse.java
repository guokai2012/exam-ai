package com.exam.ai.question.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * QuestionResponse 记录对象，封装当前业务流程中的不可变数据。
 */
@Schema(description = "题目响应")
@Builder
public record QuestionResponse(
        @Schema(description = "题目 ID")
        Long id,
        @Schema(description = "分类 ID")
        Long categoryId,
        @Schema(description = "分类名称")
        String categoryName,
        @Schema(description = "题型")
        String questionType,
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
        @Schema(description = "审核原因")
        String reviewReason,
        @Schema(description = "标签生成错误信息")
        String tagErrorMessage,
        @Schema(description = "标签重试次数")
        Integer tagRetryCount,
        @Schema(description = "是否已发送标签失败通知")
        Boolean tagNotified,
        @Schema(description = "标签名称列表")
        List<String> tags,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}

