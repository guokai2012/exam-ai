package com.exam.ai.question.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;

/**
 * QuestionStateTransitionService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface QuestionStateTransitionService {

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param questionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param currentState 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param event 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionState transit(Long questionId, String currentState, QuestionEvent event);
}
