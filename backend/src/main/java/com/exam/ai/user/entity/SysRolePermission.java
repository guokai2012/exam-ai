package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SysRolePermission 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色权限关系实体")
@TableName("sys_role_permission")
public class SysRolePermission {

    private Long roleId;
    private Long permissionId;
}

