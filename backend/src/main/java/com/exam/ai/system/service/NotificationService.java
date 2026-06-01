package com.exam.ai.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.entity.SysNotification;
import com.exam.ai.system.vo.NotificationResponse;
import com.exam.ai.system.mapper.SysNotificationMapper;
import java.time.LocalDateTime;
import org.springframework.transaction.annotation.Transactional;

/**
 * NotificationService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface NotificationService {

    String TYPE_AI_TAGGING_FAILED = "AI_TAGGING_FAILED";
    String BUSINESS_QUESTION = "QUESTION";

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @param page 业务参数，参与当前方法的校验、查询或状态变更。
     * @param size 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<NotificationResponse> list(UserPrincipal principal, long page, long size);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @param principal 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public NotificationResponse markRead(Long id, UserPrincipal principal);
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
    public void create(Long recipientId, String title, String content, String type, String businessType, Long businessId);
}
