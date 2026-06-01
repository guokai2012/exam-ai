package com.exam.ai.system.dto;

import lombok.Builder;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "站内通知响应")
@Builder
public record NotificationResponse(
        @Schema(description = "通知 ID")
        Long id,
        @Schema(description = "标题")
        String title,
        @Schema(description = "内容")
        String content,
        @Schema(description = "通知类型")
        String notificationType,
        @Schema(description = "业务类型")
        String businessType,
        @Schema(description = "业务 ID")
        Long businessId,
        @Schema(description = "是否已读")
        Boolean read,
        @Schema(description = "阅读时间")
        LocalDateTime readAt,
        @Schema(description = "创建时间")
        LocalDateTime createdAt
) {
}

