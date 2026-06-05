package com.exam.ai.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.document.dto.RetryFailedPagesRequest;
import com.exam.ai.document.vo.AnalysisResponse;
import com.exam.ai.document.vo.DocumentResponse;
import com.exam.ai.document.vo.FailedPageResponse;
import java.nio.file.Path;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * DocumentService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface DocumentService {

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param file 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentResponse upload(MultipartFile file);
    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<DocumentResponse> list(long page, long size);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentResponse detail(Long id);
    /**
     * 获取当前用户可预览的 PDF 文件路径。
     *
     * @param id 文档 ID。
     * @return PDF 文件本地路径。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在或不属于当前用户时抛出。
     */
    public Path pdfFile(Long id);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AnalysisResponse analyze(Long documentId);

    /**
     * 查询文档 AI 页解析失败列表。
     *
     * @param documentId 文档 ID。
     * @return 失败页列表。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在或不属于当前用户时抛出。
     */
    public List<FailedPageResponse> failedPages(Long documentId);

    /**
     * 批量重试用户选中的失败页。
     *
     * @param documentId 文档 ID。
     * @param request 待重试页码集合。
     * @return 最新分析结果。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在、不属于当前用户或页码非法时抛出。
     */
    public AnalysisResponse retryFailedPages(Long documentId, RetryFailedPagesRequest request);

    /**
     * 确认跳过所有失败页，使文档进入 AI 解析完成状态。
     *
     * @param documentId 文档 ID。
     * @return 最新分析结果。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在、不属于当前用户或仍存在待解析页时抛出。
     */
    public AnalysisResponse confirmSkipFailedPages(Long documentId);

    /**
     * 定时任务执行 PDF 页渲染。
     */
    public void renderPendingPdfPages();

    /**
     * 定时任务处理已完成 AI 页解析的 raw_json。
     */
    public void processCompletedRawJson();
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AnalysisResponse latestAnalysis(Long documentId);
}
