package com.exam.ai.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.entity.SysNotification;
import com.exam.ai.system.dto.NotificationResponse;
import com.exam.ai.system.mapper.SysNotificationMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

    public static final String TYPE_AI_TAGGING_FAILED = "AI_TAGGING_FAILED";
    public static final String BUSINESS_QUESTION = "QUESTION";

    private final SysNotificationMapper notificationMapper;

    public NotificationService(SysNotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    public IPage<NotificationResponse> list(UserPrincipal principal, long page, long size) {
        return notificationMapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getRecipientId, principal.userId())
                        .orderByDesc(SysNotification::getCreatedAt))
                .convert(this::toResponse);
    }

    @Transactional(rollbackFor = Exception.class)
    public NotificationResponse markRead(Long id, UserPrincipal principal) {
        SysNotification notification = notificationMapper.selectById(id);
        if (notification == null || !principal.userId().equals(notification.getRecipientId())) {
            throw BusinessException.badRequest("通知不存在");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationMapper.updateById(notification);
        }
        return toResponse(notificationMapper.selectById(id));
    }

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

