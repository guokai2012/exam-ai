package com.exam.ai.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ExamQuestionSource 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@Schema(description = "题目来源实体")
@TableName("exam_question_source")
public class ExamQuestionSource {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long documentId;
    private Long analysisId;
    private Long chunkId;
    private BigDecimal confidence;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}

