package com.exam.ai.question.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.question.entity.ExamQuestionBank;
import com.exam.ai.system.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;

/**
 * QuestionTaggingService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface QuestionTaggingService {

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void tagPendingQuestions();
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<String> analyzeTags(ExamQuestionBank question);
}
