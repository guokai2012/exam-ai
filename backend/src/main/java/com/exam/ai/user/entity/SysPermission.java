package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * SysPermission 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@Schema(description = "系统权限实体")
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private Long menuId;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Integer sortOrder;
    private Integer systemGenerated;
    private LocalDateTime createdAt;
    private LocalDateTime lastScannedAt;
}

