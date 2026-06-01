package com.exam.ai.question.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ExamQuestionTagRelation 类，承载当前分层中的业务职责。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "题目标签关系实体")
@TableName("exam_question_tag_relation")
public class ExamQuestionTagRelation {

    @TableId
    private Long questionId;
    private Long tagId;
    private LocalDateTime createdAt;
}

