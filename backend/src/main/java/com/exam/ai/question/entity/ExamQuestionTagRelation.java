package com.exam.ai.question.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.exam.ai.common.base.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * ExamQuestionTagRelation 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@Schema(description = "题目标签关系实体")
@TableName("exam_question_tag_relation")
public class ExamQuestionTagRelation extends BaseEntity {

    private Long questionId;
    private Long tagId;

    /**
     * 创建题目标签关系实体，用于批量或单条维护题目与标签之间的绑定。
     *
     * @param questionId 题目 ID，必须指向未删除的题库题目。
     * @param tagId 标签 ID，必须指向未删除且启用的题目标签。
     */
    public ExamQuestionTagRelation(Long questionId, Long tagId) {
        this.questionId = questionId;
        this.tagId = tagId;
    }
}

