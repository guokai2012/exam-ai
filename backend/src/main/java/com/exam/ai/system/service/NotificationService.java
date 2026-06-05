package com.exam.ai.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.system.vo.NotificationResponse;

/**
 * NotificationService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface NotificationService {

    String TYPE_AI_TAGGING_FAILED = "AI_TAGGING_FAILED";
    String TYPE_AI_DOCUMENT_ANALYSIS_FAILED = "AI_DOCUMENT_ANALYSIS_FAILED";
    String TYPE_AI_DOCUMENT_RENDER_FAILED = "AI_DOCUMENT_RENDER_FAILED";
    String TYPE_PERMISSION_SCAN_WARNING = "PERMISSION_SCAN_WARNING";
    String BUSINESS_QUESTION = "QUESTION";
    String BUSINESS_DOCUMENT = "DOCUMENT";
    String BUSINESS_PERMISSION_SCAN = "PERMISSION_SCAN";

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

    /**
     * 按角色编码批量创建站内通知。
     *
     * <p>用于系统任务向某类管理员或业务人员发送聚合告警；如果角色下没有用户，
     * 方法直接结束，不影响主业务流程。</p>
     *
     * @param roleCode 接收通知的角色编码，例如 ADMIN。
     * @param title 通知标题。
     * @param content 通知内容。
     * @param type 通知类型。
     * @param businessType 关联业务类型。
     * @param businessId 关联业务 ID，可为空。
     */
    void createForRole(String roleCode, String title, String content, String type, String businessType, Long businessId);
}
