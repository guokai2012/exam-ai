package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SysUserRole 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户角色关系实体")
@TableName("sys_user_role")
public class SysUserRole {

    @TableId
    private Long userId;
    private Long roleId;
}

