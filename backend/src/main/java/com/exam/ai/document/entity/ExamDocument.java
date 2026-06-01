package com.exam.ai.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ExamDocument 类，承载当前分层中的业务职责。
 */
@Data
@Schema(description = "考试文档实体")
@TableName("exam_document")
public class ExamDocument {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String originalFilename;
    private String storedFilename;
    private String fileType;
    private Long fileSize;
    private String sha256;
    private String storagePath;
    private String extractedText;
    private String status;
    private Long uploadedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

