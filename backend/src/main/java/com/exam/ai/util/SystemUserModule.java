package com.exam.ai.util;

import com.exam.ai.security.UserPrincipal;
import java.util.List;

/**
 * 系统用户模块枚举，定义无需落库的模块级虚拟系统用户。
 */
public enum SystemUserModule {

    AUTH(-900001L, "system_auth", "认证模块系统用户"),
    USER(-900002L, "system_user", "用户权限模块系统用户"),
    DOCUMENT(-900003L, "system_document", "文档模块系统用户"),
    QUESTION(-900004L, "system_question", "题库模块系统用户"),
    SYSTEM(-900005L, "system_config", "系统配置模块系统用户");

    private static final String SYSTEM_SESSION_ID = "system-session";
    private static final List<String> SYSTEM_ROLES = List.of("SYSTEM");
    private static final List<String> SYSTEM_PERMISSIONS = List.of("system:internal");

    private final Long userId;
    private final String username;
    private final String nickname;

    /**
     * 构造系统用户模块枚举项。
     * @param userId 虚拟系统用户 ID，用于审计字段。
     * @param username 虚拟系统用户名。
     * @param nickname 虚拟系统用户昵称。
     */
    SystemUserModule(Long userId, String username, String nickname) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
    }

    /**
     * 获取虚拟系统用户 ID。
     * @return 虚拟系统用户 ID。
     */
    public Long userId() {
        return userId;
    }

    /**
     * 获取虚拟系统用户名。
     * @return 虚拟系统用户名。
     */
    public String username() {
        return username;
    }

    /**
     * 获取虚拟系统用户昵称。
     * @return 虚拟系统用户昵称。
     */
    public String nickname() {
        return nickname;
    }

    /**
     * 转换为当前用户上下文对象。
     * @return 虚拟系统用户上下文。
     */
    public UserPrincipal toPrincipal() {
        return new UserPrincipal(userId, username, SYSTEM_SESSION_ID + ":" + name().toLowerCase(),
                SYSTEM_ROLES, SYSTEM_PERMISSIONS);
    }
}
