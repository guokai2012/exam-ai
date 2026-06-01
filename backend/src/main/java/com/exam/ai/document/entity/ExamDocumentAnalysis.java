package com.exam.ai.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "文档分析任务实体")
@TableName("exam_document_analysis")
public class ExamDocumentAnalysis {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long documentId;
    private String modelName;
    private String status;
    private String rawJson;
    private String errorMessage;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

