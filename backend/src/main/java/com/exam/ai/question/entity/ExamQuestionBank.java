package com.exam.ai.question.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * ExamQuestionBank 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Data
@Schema(description = "题库题目实体")
@TableName("exam_question_bank")
public class ExamQuestionBank {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long categoryId;
    private String questionType;
    private String stem;
    private String normalizedStem;
    private String stemHash;
    private String optionsJson;
    private String standardAnswer;
    private String explanation;
    private Integer difficultyStars;
    private String state;
    private Long reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewReason;
    private String tagErrorMessage;
    private Integer tagRetryCount;
    private LocalDateTime tagNotifiedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

