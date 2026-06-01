package com.exam.ai.system.service.impl;

import com.exam.ai.system.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.system.entity.SysNotification;
import com.exam.ai.system.vo.NotificationResponse;
import com.exam.ai.system.mapper.SysNotificationMapper;
import com.exam.ai.util.CurrentUserUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NotificationServiceImpl 类，承载当前分层中的业务职责。
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    public static final String TYPE_AI_TAGGING_FAILED = "AI_TAGGING_FAILED";
    public static final String BUSINESS_QUESTION = "QUESTION";

    private final SysNotificationMapper notificationMapper;

    /**
     * 构造 NotificationServiceImpl 实例并注入运行所需依赖。
     * @param notificationMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public NotificationServiceImpl(SysNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 业务参数，参与当前方法的校验、查询或状态变更。
     * @param size 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<NotificationResponse> list(long page, long size) {
        return notificationMapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getRecipientId, CurrentUserUtils.currentUserId())
                        .orderByDesc(SysNotification::getCreatedAt))
                .convert(this::toResponse);
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public NotificationResponse markRead(Long id) {
        SysNotification notification = notificationMapper.selectById(id);
        if (notification == null || !CurrentUserUtils.currentUserId().equals(notification.getRecipientId())) {
            throw BusinessException.badRequest("通知不存在");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
        return toResponse(notificationMapper.selectById(id));
    }

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param recipientId 业务参数，参与当前方法的校验、查询或状态变更。
     * @param title 业务参数，参与当前方法的校验、查询或状态变更。
     * @param content 业务参数，参与当前方法的校验、查询或状态变更。
     * @param type 业务参数，参与当前方法的校验、查询或状态变更。
     * @param businessType 业务参数，参与当前方法的校验、查询或状态变更。
     * @param businessId 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void create(Long recipientId, String title, String content, String type, String businessType, Long businessId) {
        SysNotification notification = new SysNotification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setNotificationType(type);
        notification.setBusinessType(businessType);
        notification.setBusinessId(businessId);
        notificationMapper.insert(notification);
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param notification 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private NotificationResponse toResponse(SysNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getNotificationType(),
                notification.getBusinessType(),
                notification.getBusinessId(),
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}

