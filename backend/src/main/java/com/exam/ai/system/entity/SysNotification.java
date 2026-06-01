package com.exam.ai.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "站内通知实体")
@TableName("sys_notification")
public class SysNotification {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipientId;
    private String title;
    private String content;
    private String notificationType;
    private String businessType;
    private Long businessId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}

