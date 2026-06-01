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
 * DocumentController 类，承载当前分层中的业务职责。
 */
@RestController
@RequestMapping("/api/documents")
@Tag(name = "文档分析接口", description = "教师上传文档、查看解析内容并发起 AI 题目分析")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 构造 DocumentController 实例并注入运行所需依赖。
     * @param documentService 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * 创建业务数据并完成必要的状态初始化。
     * @param file 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('document:upload')")
    @Operation(summary = "上传文档", description = "上传试题文档，系统解析文本并记录文档状态。")
    public ApiResponse<DocumentResponse> upload(@Parameter(description = "待上传的文档文件") @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(documentService.upload(file));
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 业务参数，参与当前方法的校验、查询或状态变更。
     * @param size 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('document:list')")
    @Operation(summary = "分页查询文档", description = "查询当前教师上传的文档分页列表。")
    public ApiResponse<IPage<DocumentResponse>> list(@Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") long page,
                                                     @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.ok(documentService.list(page, size));
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('document:detail')")
    @Operation(summary = "文档详情", description = "查询指定文档的基础信息和最新分析摘要。")
    public ApiResponse<DocumentResponse> detail(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.detail(id));
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping("/{id}/content")
    @PreAuthorize("hasAuthority('document:content')")
    @Operation(summary = "文档解析文本", description = "查询文档解析后的纯文本内容。")
    public ApiResponse<DocumentContentResponse> content(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.content(id));
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @PostMapping("/{id}/analysis")
    @PreAuthorize("hasAuthority('document:analyze')")
    @Operation(summary = "发起 AI 分析", description = "对文档内容进行 AI 题目识别与入库。")
    public ApiResponse<AnalysisResponse> analyze(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.analyze(id));
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @GetMapping("/{id}/analysis/latest")
    @PreAuthorize("hasAuthority('document:analysis-latest')")
    @Operation(summary = "最新分析结果", description = "查询文档最近一次 AI 分析结果。")
    public ApiResponse<AnalysisResponse> latestAnalysis(@Parameter(description = "文档 ID") @PathVariable Long id) {
        return ApiResponse.ok(documentService.latestAnalysis(id));
    }
}
