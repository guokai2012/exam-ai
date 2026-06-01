package com.exam.ai.question.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目状态机事件")
public enum QuestionEvent {
    CONFIRM_PARSED,
    REJECT_PARSED,
    START_TAGGING,
    TAG_SUCCESS,
    TAG_FAIL,
    RETRY_TAGGING
}

