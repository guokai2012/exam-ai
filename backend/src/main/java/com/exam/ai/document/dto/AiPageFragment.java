package com.exam.ai.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

/**
 * AI 页级题目片段，用于承载单页中识别出的题干、答案或解析局部内容。
 */
@Schema(description = "AI 页级题目片段")
@Builder
public record AiPageFragment(
        @Schema(description = "页码，从 1 开始")
        Integer pageNo,
        @Schema(description = "片段类型")
        String fragmentType,
        @Schema(description = "题号，无法识别时为空字符串")
        String questionNo,
        @Schema(description = "题干片段")
        String stemFragment,
        @Schema(description = "选项片段")
        List<String> options,
        @Schema(description = "答案片段")
        String answerFragment,
        @Schema(description = "解析片段")
        String explanationFragment,
        @Schema(description = "当前片段是否已经构成完整题目")
        Boolean complete,
        @Schema(description = "是否延续上一题")
        Boolean continuesPreviousQuestion
) {
}
