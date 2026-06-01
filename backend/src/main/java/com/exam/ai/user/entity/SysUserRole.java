package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SysUserRole 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "用户角色关系实体")
@TableName("sys_user_role")
public class SysUserRole extends BaseEntity {

    private Long userId;
    private Long roleId;

    /**
     * 创建用户角色关系实体，用于维护用户拥有的角色集合。
     *
     * @param userId 用户 ID，必须指向未删除用户。
     * @param roleId 角色 ID，必须指向未删除角色。
     */
    public SysUserRole(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }
}

