package com.exam.ai.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ExamQuestionSource 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "题目来源实体")
@TableName("exam_question_source")
public class ExamQuestionSource extends BaseEntity {

    private Long questionId;
    private Long documentId;
    private Long analysisId;
    private Long chunkId;
    private String sourcePageNos;
    private BigDecimal confidence;
    private Integer sortOrder;
}

