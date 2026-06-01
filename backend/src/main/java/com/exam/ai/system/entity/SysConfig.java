package com.exam.ai.system.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * SysConfig 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@Schema(description = "系统配置实体")
@TableName("sys_config")
public class SysConfig {

    @TableId
    private String configKey;
    private String configValue;
    private String configName;
    private String description;
    private String valueType;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

