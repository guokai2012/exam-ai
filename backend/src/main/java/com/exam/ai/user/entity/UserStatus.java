package com.exam.ai.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * UserStatus 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Schema(description = "用户状态常量")
public final class UserStatus {

    public static final int DISABLED = 0;
    public static final int ENABLED = 1;

    private UserStatus() {
    }
}

