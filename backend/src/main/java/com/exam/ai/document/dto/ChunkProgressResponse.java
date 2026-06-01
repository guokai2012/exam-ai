package com.exam.ai.document.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "文档分块分析进度")
@Builder
public record ChunkProgressResponse(
        @Schema(description = "总分块数")
        Integer total,
        @Schema(description = "成功分块数")
        Integer success,
        @Schema(description = "失败分块数")
        Integer failed,
        @Schema(description = "待处理分块数")
        Integer pending,
        @Schema(description = "处理中分块数")
        Integer processing,
        @Schema(description = "超长分块数")
        Integer oversized,
        @Schema(description = "最近错误信息")
        String latestErrorMessage
) {
}

