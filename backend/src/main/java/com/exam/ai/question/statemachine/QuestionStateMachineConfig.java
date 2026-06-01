package com.exam.ai.question.statemachine;

import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import java.util.EnumSet;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

@Configuration
@EnableStateMachineFactory
public class QuestionStateMachineConfig extends EnumStateMachineConfigurerAdapter<QuestionState, QuestionEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<QuestionState, QuestionEvent> states) throws Exception {
        states.withStates()
                .initial(QuestionState.PARSE_PENDING_CONFIRM)
                .states(EnumSet.allOf(QuestionState.class));
    }

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

