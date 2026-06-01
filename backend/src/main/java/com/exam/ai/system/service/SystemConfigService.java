package com.exam.ai.system.service;

import com.exam.ai.system.vo.SystemConfigResponse;
import com.exam.ai.system.vo.SystemConfigUpdateResult;
import com.exam.ai.system.dto.UpdateSystemConfigRequest;
import java.util.List;

/**
 * SystemConfigService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface SystemConfigService {

    String AI_TAGGING_MAX_RETRIES = "ai.tagging.max-retries";
    String AI_DOCUMENT_ANALYSIS_MAX_RETRIES = "ai.document-analysis.max-retries";

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<SystemConfigResponse> listConfigs();
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public int aiTaggingMaxRetries();
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public int aiDocumentAnalysisMaxRetries();
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param key 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public SystemConfigUpdateResult updateConfig(String key, UpdateSystemConfigRequest request);
}
