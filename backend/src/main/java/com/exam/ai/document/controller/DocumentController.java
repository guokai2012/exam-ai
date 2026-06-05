package com.exam.ai.document.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.document.dto.RetryFailedPagesRequest;
import com.exam.ai.document.vo.AnalysisResponse;
import com.exam.ai.document.vo.DocumentResponse;
import com.exam.ai.document.service.DocumentService;
import com.exam.ai.document.vo.FailedPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档分析接口控制器，负责教师 PDF 上传、PDF 预览、页级 AI 分析和失败页处理入口。
 *
 * <p>控制器仅接收 HTTP 参数并返回统一响应，文件落盘、文本抽取、分析状态流转和题目入库由
 * {@link DocumentService} 统一处理。</p>
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档分析接口", description = "教师上传文档、查看解析内容并发起 AI 题目分析")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 构造文档分析接口控制器。
     *
     * @param documentService 文档上传、查询和分析业务服务。
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 上传 PDF 文档。
     *
     * @param file 待上传的 PDF 文件。
     * @return 文档基础信息和初始解析状态。
     * @throws com.exam.ai.common.exception.BusinessException 当文件为空、类型不支持、大小超限或文本提取失败时抛出。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('document:upload')")
    @Operation(summary = "上传文档", description = "上传 PDF 试题文档，系统保存文件并等待后台生成页图片。")
    public ApiResponse<DocumentResponse> upload(@Parameter(description = "待上传的 PDF 文件") @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(documentService.upload(file));
    }

    /**
     * 分页查询当前用户上传的文档。
     *
     * @param page 页码，从 1 开始。
     * @param size 每页数量。
     * @return 当前用户的文档分页数据。
     * @throws com.exam.ai.common.exception.BusinessException 当用户上下文缺失时抛出。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('document:list')")
    @Operation(summary = "分页查询文档", description = "查询当前教师上传的文档分页列表。")
    public ApiResponse<IPage<DocumentResponse>> list(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                     @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(documentService.list(page, size));
    }

    /**
     * 查询文档详情。
     *
     * @param id 文档 ID。
     * @return 文档基础信息和最新分析摘要。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在或不属于当前用户时抛出。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('document:detail')")
    @Operation(summary = "文档详情", description = "查询指定文档的基础信息和最新分析摘要。")
    public ApiResponse<DocumentResponse> detail(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.detail(id));
    }

    /**
     * 预览 PDF 原文件。
     *
     * @param id 文档 ID。
     * @return PDF 文件流。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在、不属于当前用户或文件缺失时抛出。
     */
    @GetMapping("/{id}/file")
    @PreAuthorize("hasAuthority('document:file')")
    @Operation(summary = "PDF 文件预览", description = "以内联文件流方式返回当前用户可访问的 PDF 原文件。")
    public ResponseEntity<Resource> file(@Parameter(description = "文档 ID") @PathVariable Long id) {
        Path pdfPath = documentService.pdfFile(id);
        FileSystemResource resource = new FileSystemResource(pdfPath);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(pdfPath.getFileName().toString())
                        .build()
                        .toString())
                .body(resource);
    }

    /**
     * 发起或继续文档 AI 题目分析。
     *
     * @param id 文档 ID。
     * @return 分析批次、分片进度和识别出的题目结果。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在、不属于当前用户或当前状态不允许分析时抛出。
     */
    @PostMapping("/{id}/analysis")
    @PreAuthorize("hasAuthority('document:analyze')")
    @Operation(summary = "发起 AI 分析", description = "对文档内容进行 AI 题目识别与入库。")
    public ApiResponse<AnalysisResponse> analyze(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.analyze(id));
    }

    /**
     * 查询 AI 页面解析失败列表。
     *
     * @param id 文档 ID。
     * @return 当前文档所有失败页。
     */
    @GetMapping("/{id}/analysis/failed-pages")
    @PreAuthorize("hasAuthority('document:analysis-failed-pages')")
    @Operation(summary = "失败页列表", description = "查询 PDF AI 页级解析失败页面，供用户批量重试或确认跳过。")
    public ApiResponse<List<FailedPageResponse>> failedPages(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.failedPages(id));
    }

    /**
     * 批量重试用户选择的失败页。
     *
     * @param id 文档 ID。
     * @param request 待重试页码集合。
     * @return 最新分析状态。
     */
    @PostMapping("/{id}/analysis/pages/retry")
    @PreAuthorize("hasAuthority('document:analysis-retry-pages')")
    @Operation(summary = "批量重试失败页", description = "按用户多选页码重试 PDF AI 页级解析。")
    public ApiResponse<AnalysisResponse> retryFailedPages(@Parameter(description = "文档 ID") @PathVariable Long id,
                                                          @Valid @org.springframework.web.bind.annotation.RequestBody RetryFailedPagesRequest request) {
        return ApiResponse.ok(documentService.retryFailedPages(id, request));
    }

    /**
     * 确认跳过所有失败页。
     *
     * @param id 文档 ID。
     * @return 最新分析状态。
     */
    @PostMapping("/{id}/analysis/confirm-skip-failed-pages")
    @PreAuthorize("hasAuthority('document:analysis-confirm-skip')")
    @Operation(summary = "确认跳过失败页", description = "将剩余失败页标记为跳过，使文档进入 AI 解析完成状态。")
    public ApiResponse<AnalysisResponse> confirmSkipFailedPages(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.confirmSkipFailedPages(id));
    }

    /**
     * 查询文档最近一次 AI 分析结果。
     *
     * @param id 文档 ID。
     * @return 最近一次分析批次和题目结果。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在、不属于当前用户或没有分析记录时抛出。
     */
    @GetMapping("/{id}/analysis/latest")
    @PreAuthorize("hasAuthority('document:analysis-latest')")
    @Operation(summary = "最新分析结果", description = "查询文档最近一次 AI 分析结果。")
    public ApiResponse<AnalysisResponse> latestAnalysis(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.latestAnalysis(id));
    }
}
