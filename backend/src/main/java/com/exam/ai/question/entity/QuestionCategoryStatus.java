package com.exam.ai.question.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目分类状态常量")
public final class QuestionCategoryStatus {

    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    private QuestionCategoryStatus() {
    }
}

