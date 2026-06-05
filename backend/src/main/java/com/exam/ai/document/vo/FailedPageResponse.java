package com.exam.ai.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * PDF 页级 AI 解析失败记录响应。
 *
 * @param pageNo 页码，从 1 开始。
 * @param retryCount 当前页累计重试次数。
 * @param errorMessage 最近一次解析失败原因。
 * @param pageImagePath 后端保存的页面图片路径，仅用于排查和后续扩展预览。
 * @param updatedAt 最近更新时间。
 */
@Schema(description = "PDF 页级 AI 解析失败记录")
public record FailedPageResponse(
        @Schema(description = "页码，从 1 开始")
        Integer pageNo,
        @Schema(description = "累计重试次数")
        Integer retryCount,
        @Schema(description = "失败原因")
        String errorMessage,
        @Schema(description = "页面图片保存路径")
        String pageImagePath,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {
}
