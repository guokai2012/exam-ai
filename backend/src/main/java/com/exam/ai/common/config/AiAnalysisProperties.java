package com.exam.ai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AiAnalysisProperties 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiAnalysisProperties {

    private int maxInputChars = 12000;

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public int getMaxInputChars() {
        return maxInputChars;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param maxInputChars 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void setMaxInputChars(int maxInputChars) {
        this.maxInputChars = maxInputChars;
    }
}
