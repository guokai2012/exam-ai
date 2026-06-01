package com.exam.ai.document.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "文档响应")
@Builder
public record DocumentResponse(
        @Schema(description = "文档 ID")
        Long id,
        @Schema(description = "原始文件名")
        String originalFilename,
        @Schema(description = "文件类型")
        String fileType,
        @Schema(description = "文件大小，单位字节")
        Long fileSize,
        @Schema(description = "文件 SHA-256")
        String sha256,
        @Schema(description = "文档状态")
        String status,
        @Schema(description = "上传人用户 ID")
        Long uploadedBy,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "最新分析摘要")
        AnalysisSummary latestAnalysis
) {
}

