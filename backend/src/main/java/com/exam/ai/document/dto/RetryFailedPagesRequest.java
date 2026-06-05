package com.exam.ai.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量重试失败 PDF 页面的请求参数。
 *
 * @param pageNos 用户在失败页确认界面选择的页码集合，页码从 1 开始。
 */
@Schema(description = "批量重试失败页请求")
public record RetryFailedPagesRequest(
        @Schema(description = "待重试页码，页码从 1 开始")
        @NotEmpty(message = "请选择要重试的失败页")
        List<Integer> pageNos
) {
}
