package com.exam.ai.document.scheduler;

import com.exam.ai.document.service.DocumentService;
import com.exam.ai.util.CurrentUserUtils;
import com.exam.ai.util.SystemUserModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PDF 页面图片渲染定时任务。
 *
 * <p>任务以文档模块虚拟系统用户运行，扫描刚上传的 PDF 文档并生成页图片 chunk。
 * 单次任务异常只记录日志，不影响后续调度。</p>
 */
@Component
public class PdfPageRenderScheduler {

    private static final Logger log = LoggerFactory.getLogger(PdfPageRenderScheduler.class);

    private final DocumentService documentService;

    /**
     * 构造 PDF 页面渲染任务。
     *
     * @param documentService 文档服务。
     */
    public PdfPageRenderScheduler(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 定时扫描待渲染 PDF 文档。
     */
    @Scheduled(fixedDelayString = "${app.document.page-render-delay:30000}")
    public void renderPages() {
        try {
            CurrentUserUtils.runAsSystem(SystemUserModule.DOCUMENT, documentService::renderPendingPdfPages);
        } catch (Exception ex) {
            log.warn("PDF 页面渲染定时任务执行失败", ex);
        }
    }
}
