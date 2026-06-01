package com.exam.ai.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Schema(description = "文档分析分块实体")
@TableName("exam_document_analysis_chunk")
public class ExamDocumentAnalysisChunk {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long analysisId;
    private Long documentId;
    private Integer chunkIndex;
    private String chunkText;
    private String chunkHash;
    private Integer startOffset;
    private Integer endOffset;
    private Integer questionCountEstimate;
    private Boolean oversized;
    private String status;
    private Integer retryCount;
    private String rawJson;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

