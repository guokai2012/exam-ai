package com.exam.ai.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ExamQuestionCategory 类，承载当前分层中的业务职责。
 */
@Data
@Schema(description = "题目分类实体")
@TableName("exam_question_category")
public class ExamQuestionCategory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;
    private String description;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

