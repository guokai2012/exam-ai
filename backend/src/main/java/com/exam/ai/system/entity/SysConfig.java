package com.exam.ai.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * SysConfig 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统配置实体")
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private String configKey;
    private String configValue;
    private String configName;
    private String description;
    private String valueType;
}

