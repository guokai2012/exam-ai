package com.exam.ai.document.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * AnalysisResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
@Schema(description = "文档分析响应")
@Builder
public record AnalysisResponse(
        @Schema(description = "分析 ID")
        Long id,
        @Schema(description = "文档 ID")
        Long documentId,
        @Schema(description = "分析状态")
        String status,
        @Schema(description = "模型名称")
        String modelName,
        @Schema(description = "错误信息")
        String errorMessage,
        @Schema(description = "分块处理进度")
        ChunkProgressResponse chunkProgress,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "识别出的题目列表")
        List<QuestionAnalysisResponse> questions
) {
}

