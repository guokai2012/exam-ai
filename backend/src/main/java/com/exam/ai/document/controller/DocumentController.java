package com.exam.ai.document.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.common.result.ApiResponse;
import com.exam.ai.document.vo.AnalysisResponse;
import com.exam.ai.document.vo.DocumentContentResponse;
import com.exam.ai.document.vo.DocumentResponse;
import com.exam.ai.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档分析接口控制器，负责教师文档上传、文档列表、文本预览和 AI 分析入口。
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
     * 上传文档并提取原始文本。
     *
     * @param file 待上传的 md、pdf、doc 或 docx 文件。
     * @return 文档基础信息和初始解析状态。
     * @throws com.exam.ai.common.exception.BusinessException 当文件为空、类型不支持、大小超限或文本提取失败时抛出。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('document:upload')")
    @Operation(summary = "上传文档", description = "上传试题文档，系统解析文本并记录文档状态。")
    public ApiResponse<DocumentResponse> upload(@Parameter(description = "待上传的文档文件") @RequestParam("file") MultipartFile file) {
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
     * 查询文档提取后的纯文本内容。
     *
     * @param id 文档 ID。
     * @return 文档文本预览内容。
     * @throws com.exam.ai.common.exception.BusinessException 当文档不存在、不属于当前用户或尚未提取文本时抛出。
     */
    @GetMapping("/{id}/content")
    @PreAuthorize("hasAuthority('document:content')")
    @Operation(summary = "文档解析文本", description = "查询文档解析后的纯文本内容。")
    public ApiResponse<DocumentContentResponse> content(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.content(id));
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
