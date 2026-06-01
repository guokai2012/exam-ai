package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SysUser 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统用户实体")
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String passwordHash;
    private String nickname;
    private Integer status;
    private Boolean forcePasswordChange;
    private LocalDateTime lastLoginAt;
    private LocalDateTime passwordUpdatedAt;
}

