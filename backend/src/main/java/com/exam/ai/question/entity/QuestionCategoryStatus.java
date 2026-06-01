package com.exam.ai.question.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * QuestionCategoryStatus 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Schema(description = "题目分类状态常量")
public final class QuestionCategoryStatus {

    public static final int ENABLED = 1;
    public static final int DISABLED = 0;

    private QuestionCategoryStatus() {
    }
}

