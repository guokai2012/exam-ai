package com.exam.ai.question.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@Service
public class QuestionStateTransitionService {

    private final StateMachineFactory<QuestionState, QuestionEvent> stateMachineFactory;

    public QuestionStateTransitionService(StateMachineFactory<QuestionState, QuestionEvent> stateMachineFactory) {
        this.stateMachineFactory = stateMachineFactory;
    }

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

