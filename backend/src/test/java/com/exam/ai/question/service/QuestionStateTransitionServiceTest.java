package com.exam.ai.question.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import com.exam.ai.question.statemachine.QuestionStateMachineConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = {QuestionStateMachineConfig.class, QuestionStateTransitionService.class})
class QuestionStateTransitionServiceTest {

    @Autowired
    private QuestionStateTransitionService transitionService;

    @Test
    void shouldTransitThroughReviewAndTaggingFlow() {
        QuestionState reviewed = transitionService.transit(1L, QuestionState.PARSE_PENDING_CONFIRM.name(), QuestionEvent.CONFIRM_PARSED);
        QuestionState processing = transitionService.transit(1L, reviewed.name(), QuestionEvent.START_TAGGING);
        QuestionState available = transitionService.transit(1L, processing.name(), QuestionEvent.TAG_SUCCESS);

        assertThat(reviewed).isEqualTo(QuestionState.TAG_PENDING);
        assertThat(processing).isEqualTo(QuestionState.TAG_PROCESSING);
        assertThat(available).isEqualTo(QuestionState.AVAILABLE);
    }

    @Test
    void shouldRejectIllegalTransition() {
        assertThatThrownBy(() -> transitionService.transit(
                2L,
                QuestionState.PARSE_PENDING_CONFIRM.name(),
                QuestionEvent.TAG_SUCCESS
        )).isInstanceOf(BusinessException.class)
                .hasMessage("题目状态不允许执行该操作");
    }

    @Test
    void shouldAllowRetryAfterTagFailed() {
        QuestionState failed = transitionService.transit(3L, QuestionState.TAG_PROCESSING.name(), QuestionEvent.TAG_FAIL);
        QuestionState retrying = transitionService.transit(3L, failed.name(), QuestionEvent.RETRY_TAGGING);

        assertThat(failed).isEqualTo(QuestionState.TAG_FAILED);
        assertThat(retrying).isEqualTo(QuestionState.TAG_PROCESSING);
    }
}

