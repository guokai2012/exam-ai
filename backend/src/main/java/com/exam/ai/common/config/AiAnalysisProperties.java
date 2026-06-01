package com.exam.ai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AiAnalysisProperties 类，承载当前分层中的业务职责。
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiAnalysisProperties {

    private int maxInputChars = 12000;

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public int getMaxInputChars() {
        return maxInputChars;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param maxInputChars 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }
}
