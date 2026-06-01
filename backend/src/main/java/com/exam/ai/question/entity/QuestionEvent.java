package com.exam.ai.question.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * QuestionEvent 枚举，约束当前业务场景允许使用的固定状态值。
 */
@Schema(description = "题目状态机事件")
public enum QuestionEvent {
    CONFIRM_PARSED,
    REJECT_PARSED,
    START_TAGGING,
    TAG_SUCCESS,
    TAG_FAIL,
    RETRY_TAGGING
}

