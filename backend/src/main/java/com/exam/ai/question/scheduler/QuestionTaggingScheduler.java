package com.exam.ai.question.scheduler;

import com.exam.ai.question.service.QuestionTaggingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QuestionTaggingScheduler {

    private static final Logger log = LoggerFactory.getLogger(QuestionTaggingScheduler.class);

    private final QuestionTaggingService questionTaggingService;

    /**
     * 创建题目标签定时任务调度器。
     *
     * @param questionTaggingService 题目标签业务服务
     */
    public QuestionTaggingScheduler(QuestionTaggingService questionTaggingService) {
        this.questionTaggingService = questionTaggingService;
    }

    /**
     * 按配置间隔扫描待打标签题目，并触发 AI 标签分析流程。
     */
    @Scheduled(fixedDelayString = "${app.ai.tagging-delay:30000}")
    public void tagPendingQuestions() {
        try {
            // 定时任务只负责调度入口，具体业务状态流转集中在 service 中处理。
            questionTaggingService.tagPendingQuestions();
        } catch (Exception ex) {
            log.warn("题目标签定时任务执行失败", ex);
        }
    }
}
