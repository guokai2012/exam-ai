package com.exam.ai.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ExamQuestionTag 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "题目标签实体")
@TableName("exam_question_tag")
public class ExamQuestionTag extends BaseEntity {

    private String tagName;
    private String description;
    private Integer status;
}

