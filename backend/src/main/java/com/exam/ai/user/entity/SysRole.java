package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * SysRole 类，承载当前分层中的业务职责。
 */
@Data
@Schema(description = "系统角色实体")
@TableName("sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String roleCode;
    private String roleName;
    private LocalDateTime createdAt;
}

