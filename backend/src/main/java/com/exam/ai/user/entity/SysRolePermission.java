package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "角色权限关系实体")
@TableName("sys_role_permission")
public class SysRolePermission {

    private Long roleId;
    private Long permissionId;
}

