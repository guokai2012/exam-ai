package com.exam.ai.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SysPermission 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统权限实体")
@TableName("sys_permission")
public class SysPermission extends BaseEntity {

    private Long parentId;
    private Long menuId;
    private String permissionCode;
    private String permissionName;
    private String permissionType;
    private Integer sortOrder;
    private Integer systemGenerated;
    private LocalDateTime lastScannedAt;
}

