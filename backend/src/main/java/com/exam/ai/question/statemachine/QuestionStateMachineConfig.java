package com.exam.ai.question.statemachine;

import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import java.util.EnumSet;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

/**
 * QuestionStateMachineConfig 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Configuration
@EnableStateMachineFactory
public class QuestionStateMachineConfig extends EnumStateMachineConfigurerAdapter<QuestionState, QuestionEvent> {

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param S 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param states 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public void configure(StateMachineStateConfigurer<QuestionState, QuestionEvent> states) throws Exception {
        states.withStates()
                .initial(QuestionState.PARSE_PENDING_CONFIRM)
                .states(EnumSet.allOf(QuestionState.class));
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param S 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param transitions 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Override
    public void configure(StateMachineTransitionConfigurer<QuestionState, QuestionEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(QuestionState.PARSE_PENDING_CONFIRM).target(QuestionState.TAG_PENDING).event(QuestionEvent.CONFIRM_PARSED)
                .and()
                .withExternal()
                    .source(QuestionState.PARSE_PENDING_CONFIRM).target(QuestionState.PARSE_REJECTED).event(QuestionEvent.REJECT_PARSED)
                .and()
                .withExternal()
                    .source(QuestionState.TAG_PENDING).target(QuestionState.TAG_PROCESSING).event(QuestionEvent.START_TAGGING)
                .and()
                .withExternal()
                    .source(QuestionState.TAG_FAILED).target(QuestionState.TAG_PROCESSING).event(QuestionEvent.RETRY_TAGGING)
                .and()
                .withExternal()
                    .source(QuestionState.TAG_PROCESSING).target(QuestionState.AVAILABLE).event(QuestionEvent.TAG_SUCCESS)
                .and()
                .withExternal()
                    .source(QuestionState.TAG_PROCESSING).target(QuestionState.TAG_FAILED).event(QuestionEvent.TAG_FAIL);
    }
}

