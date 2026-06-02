package com.exam.ai.system.service.impl;

import com.exam.ai.system.service.NotificationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.system.entity.SysNotification;
import com.exam.ai.system.vo.NotificationResponse;
import com.exam.ai.system.mapper.SysNotificationMapper;
import com.exam.ai.user.entity.SysUser;
import com.exam.ai.user.mapper.SysUserMapper;
import com.exam.ai.util.CurrentUserUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NotificationServiceImpl 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    public static final String TYPE_AI_TAGGING_FAILED = "AI_TAGGING_FAILED";
    public static final String TYPE_PERMISSION_SCAN_WARNING = "PERMISSION_SCAN_WARNING";
    public static final String BUSINESS_QUESTION = "QUESTION";
    public static final String BUSINESS_PERMISSION_SCAN = "PERMISSION_SCAN";

    private final SysNotificationMapper notificationMapper;
    private final SysUserMapper userMapper;

    /**
     * 构造 NotificationServiceImpl 实例并注入运行所需依赖。
     * @param notificationMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userMapper 用户表访问器，用于按角色查询通知接收人。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public NotificationServiceImpl(SysNotificationMapper notificationMapper, SysUserMapper userMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<NotificationResponse> list(long page, long size) {
        return notificationMapper.selectPage(Page.of(page, size), new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getRecipientId, CurrentUserUtils.currentUserId())
                        .orderByDesc(SysNotification::getCreateTime))
                .convert(this::toResponse);
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
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
     * 创建业务数据并完成必要的默认状态初始化。
     * @param recipientId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param title 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param content 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param type 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param businessType 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param businessId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
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
     * 按角色编码批量创建站内通知。
     *
     * @param roleCode 接收通知的角色编码。
     * @param title 通知标题。
     * @param content 通知内容。
     * @param type 通知类型。
     * @param businessType 关联业务类型。
     * @param businessId 关联业务 ID，可为空。
     */
    @Transactional(rollbackFor = Exception.class)
    public void createForRole(String roleCode, String title, String content, String type, String businessType, Long businessId) {
        // 通知按用户维度落库，便于每个管理员独立标记已读。
        for (SysUser user : userMapper.selectByRoleCode(roleCode)) {
            create(user.getId(), title, content, type, businessType, businessId);
        }
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param notification 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
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
                notification.getCreateTime()
        );
    }
}

