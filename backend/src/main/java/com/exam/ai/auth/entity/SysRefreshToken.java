package com.exam.ai.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * SysRefreshToken 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@Schema(description = "刷新令牌实体")
@TableName("sys_refresh_token")
public class SysRefreshToken {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String tokenHash;
    private Long userId;
    private String sessionId;
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private LocalDateTime revokedAt;
    private String replacedByHash;
    private String createdIp;
    private String userAgent;
    private LocalDateTime createdAt;
}

