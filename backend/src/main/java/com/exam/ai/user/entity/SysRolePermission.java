package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SysRolePermission 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "角色权限关系实体")
@TableName("sys_role_permission")
public class SysRolePermission extends BaseEntity {

    private Long roleId;
    private Long permissionId;

    /**
     * 创建角色权限关系实体，用于维护角色与可分配动作权限之间的绑定。
     *
     * @param roleId 角色 ID，必须指向未删除角色。
     * @param permissionId 权限 ID，必须指向未删除动作权限。
     */
    public SysRolePermission(Long roleId, Long permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }
}

