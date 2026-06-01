package com.exam.ai.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.common.api.ApiResponse;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.dto.NotificationResponse;
import com.exam.ai.system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "站内通知接口", description = "查询站内通知并标记已读")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('notification:list')")
    @Operation(summary = "分页查询通知", description = "查询当前用户收到的站内通知。")
    public ApiResponse<IPage<NotificationResponse>> notifications(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
                                                                  @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                                  @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(notificationService.list(principal, page, size));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:mark-read')")
    @Operation(summary = "标记通知已读", description = "将指定通知标记为已读。")
    public ApiResponse<NotificationResponse> read(@Parameter(description = "通知 ID") @PathVariable Long id,
                                                  @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(notificationService.markRead(id, principal));
    }
}
