package com.exam.ai.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.util.CurrentUserUtils;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * MyBatis-Plus 审计字段自动填充器，统一维护 BaseEntity 中的创建人与更新人信息。
 *
 * <p>业务代码新增或更新实体时无需手工写入 {@code createId/createTime/updateId/updateTime}。
 * 登录用户、子线程传播用户和定时任务虚拟系统用户都会从 {@link CurrentUserUtils} 读取；公开注册等
 * 无登录上下文入口使用 {@code 0} 作为系统初始化来源，避免审计填充影响主流程。</p>
 */
@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    private static final Long SYSTEM_INIT_USER_ID = 0L;

    /**
     * 插入实体时写入创建审计字段和首次更新审计字段。
     *
     * @param metaObject MyBatis-Plus 当前正在处理的实体元对象，用于判断和写入公共审计字段。
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = resolveCurrentUserId();
        // 创建与首次更新在插入阶段保持一致，方便后续审计追踪数据初始来源。
        strictInsertFill(metaObject, "createId", Long.class, currentUserId);
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateId", Long.class, currentUserId);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "deleted", Long.class, 0L);
    }

    /**
     * 更新实体时刷新更新审计字段。
     *
     * @param metaObject MyBatis-Plus 当前正在处理的实体元对象，用于写入更新人和更新时间。
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新审计字段必须反映本次操作发起人，不能沿用实体上旧值。
        setFieldValByName("updateId", resolveCurrentUserId(), metaObject);
        setFieldValByName("updateTime", LocalDateTime.now(), metaObject);
    }

    /**
     * 解析当前用户 ID，供审计字段填充使用。
     *
     * @return 当前登录用户、虚拟系统用户或系统初始化用户 ID。
     */
    private Long resolveCurrentUserId() {
        return CurrentUserUtils.getCurrentUser()
                .map(UserPrincipal::userId)
                .orElse(SYSTEM_INIT_USER_ID);
    }
}
