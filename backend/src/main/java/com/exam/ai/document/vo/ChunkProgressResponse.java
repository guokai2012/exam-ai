package com.exam.ai.document.vo;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * ChunkProgressResponse 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
 */
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
        @Schema(description = "已跳过页数")
        Integer skipped,
        @Schema(description = "最近错误信息")
        String latestErrorMessage
) {
}

