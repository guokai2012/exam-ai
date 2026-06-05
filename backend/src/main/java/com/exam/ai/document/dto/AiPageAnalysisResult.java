package com.exam.ai.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

/**
 * AI 单页分析结果，用于描述页面类型和页面内题目片段。
 */
@Schema(description = "AI 单页分析结果")
@Builder
public record AiPageAnalysisResult(
        @Schema(description = "页码，从 1 开始")
        Integer pageNo,
        @Schema(description = "页面类型")
        String pageType,
        @Schema(description = "页面题目片段")
        List<AiPageFragment> fragments
) {
}
