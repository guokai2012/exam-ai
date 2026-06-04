package com.exam.ai.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ExamDocument 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "考试文档实体")
@TableName("exam_document")
public class ExamDocument extends BaseEntity {

    private String originalFilename;
    private String storedFilename;
    private String fileType;
    private Long fileSize;
    private String sha256;
    private String storagePath;
    private String extractedText;
    private String status;
}

