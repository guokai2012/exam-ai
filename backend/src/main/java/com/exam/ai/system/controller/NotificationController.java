package com.exam.ai.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.system.vo.NotificationResponse;
import com.exam.ai.system.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * NotificationController 类，承载当前分层中的业务职责。
 */
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "站内通知接口", description = "查询站内通知并标记已读")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 构造 NotificationController 实例并注入运行所需依赖。
     * @param notificationService 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param page 业务参数，参与当前方法的校验、查询或状态变更。
     * @param size 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('notification:list')")
    @Operation(summary = "分页查询通知", description = "查询当前用户收到的站内通知。")
    public ApiResponse<IPage<NotificationResponse>> notifications(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                                  @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(notificationService.list(page, size));
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PostMapping("/{id}/read")
    @PreAuthorize("hasAuthority('notification:mark-read')")
    @Operation(summary = "标记通知已读", description = "将指定通知标记为已读。")
    public ApiResponse<NotificationResponse> read(@Parameter(description = "通知 ID") @PathVariable Long id) {
        return ApiResponse.ok(notificationService.markRead(id));
    }
}
