package com.exam.ai.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.system.vo.NotificationResponse;

/**
 * NotificationService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface NotificationService {

    String TYPE_AI_TAGGING_FAILED = "AI_TAGGING_FAILED";
    String BUSINESS_QUESTION = "QUESTION";

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<NotificationResponse> list(long page, long size);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public NotificationResponse markRead(Long id);
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
    public void create(Long recipientId, String title, String content, String type, String businessType, Long businessId);
}
