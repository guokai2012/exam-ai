package com.exam.ai.document.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "文档分析摘要")
@Builder
public record AnalysisSummary(
        @Schema(description = "分析 ID")
        Long id,
        @Schema(description = "分析状态")
        String status,
        @Schema(description = "模型名称")
        String modelName,
        @Schema(description = "题目数量")
        Integer questionCount,
        @Schema(description = "错误信息")
        String errorMessage,
        @Schema(description = "分块处理进度")
        ChunkProgressResponse chunkProgress,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}

