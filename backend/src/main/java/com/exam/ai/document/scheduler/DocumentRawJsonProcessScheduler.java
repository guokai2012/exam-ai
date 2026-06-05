package com.exam.ai.document.scheduler;

import com.exam.ai.document.service.DocumentService;
import com.exam.ai.util.CurrentUserUtils;
import com.exam.ai.util.SystemUserModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 文档 AI raw_json 后处理定时任务。
 *
 * <p>任务只处理已经进入 AI_PARSE_COMPLETE 的文档，将成功页 raw_json 统一转为待确认题目。
 * 未完成或仍存在失败页的文档不会被处理。</p>
 */
@Component
public class DocumentRawJsonProcessScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentRawJsonProcessScheduler.class);

    private final DocumentService documentService;

    /**
     * 构造文档 raw_json 后处理任务。
     *
     * @param documentService 文档服务。
     */
    public DocumentRawJsonProcessScheduler(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 定时处理已完成页级 AI 解析的文档。
     */
    @Scheduled(fixedDelayString = "${app.document.raw-json-process-delay:30000}")
    public void processRawJson() {
        try {
            CurrentUserUtils.runAsSystem(SystemUserModule.DOCUMENT, documentService::processCompletedRawJson);
        } catch (Exception ex) {
            log.warn("文档 AI raw_json 后处理定时任务执行失败", ex);
        }
    }
}
