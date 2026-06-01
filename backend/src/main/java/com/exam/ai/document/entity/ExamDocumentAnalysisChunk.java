package com.exam.ai.document.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ExamDocumentAnalysisChunk 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文档分析分块实体")
@TableName("exam_document_analysis_chunk")
public class ExamDocumentAnalysisChunk extends BaseEntity {

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
}

