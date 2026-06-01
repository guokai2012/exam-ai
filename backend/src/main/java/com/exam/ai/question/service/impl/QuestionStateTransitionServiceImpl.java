package com.exam.ai.question.service.impl;

import com.exam.ai.question.service.QuestionStateTransitionService;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

/**
 * QuestionStateTransitionServiceImpl 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class QuestionStateTransitionServiceImpl implements QuestionStateTransitionService {

    private final StateMachineFactory<QuestionState, QuestionEvent> stateMachineFactory;

    /**
     * 构造 QuestionStateTransitionServiceImpl 实例并注入运行所需依赖。
     * @param S 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param stateMachineFactory 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionStateTransitionServiceImpl(StateMachineFactory<QuestionState, QuestionEvent> stateMachineFactory) {
        this.stateMachineFactory = stateMachineFactory;
    }

    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param questionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param currentState 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param event 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionState transit(Long questionId, String currentState, QuestionEvent event) {
        QuestionState state = QuestionState.valueOf(currentState);
        StateMachine<QuestionState, QuestionEvent> machine = stateMachineFactory.getStateMachine("question-" + questionId);
        machine.stopReactively().block();
        machine.getStateMachineAccessor().doWithAllRegions(access -> access
                .resetStateMachineReactively(new DefaultStateMachineContext<>(state, null, null, null))
                .block());
        machine.startReactively().block();
        Boolean accepted = machine.sendEvent(MessageBuilder.withPayload(event).build());
        if (!Boolean.TRUE.equals(accepted)) {
            machine.stopReactively().block();
            throw BusinessException.badRequest("题目状态不允许执行该操作");
        }
        QuestionState next = machine.getState().getId();
        machine.stopReactively().block();
        return next;
    }
}

