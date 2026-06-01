package com.exam.ai.question.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目状态")
public enum QuestionState {
    PARSE_PENDING_CONFIRM,
    PARSE_REJECTED,
    TAG_PENDING,
    TAG_PROCESSING,
    TAG_FAILED,
    AVAILABLE
}

