package com.exam.ai.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.config.DocumentProperties;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiAssembledQuestionItem;
import com.exam.ai.document.dto.AiDocumentAssembleResult;
import com.exam.ai.document.dto.AiPageAnalysisResult;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.document.dto.RetryFailedPagesRequest;
import com.exam.ai.document.entity.AnalysisChunkStatus;
import com.exam.ai.document.entity.AnalysisStatus;
import com.exam.ai.document.entity.DocumentStatus;
import com.exam.ai.document.entity.ExamDocument;
import com.exam.ai.document.entity.ExamDocumentAnalysis;
import com.exam.ai.document.entity.ExamDocumentAnalysisChunk;
import com.exam.ai.document.mapper.ExamDocumentAnalysisChunkMapper;
import com.exam.ai.document.mapper.ExamDocumentAnalysisMapper;
import com.exam.ai.document.mapper.ExamDocumentMapper;
import com.exam.ai.document.service.DocumentQuestionAssemblerClient;
import com.exam.ai.document.service.DocumentFileService;
import com.exam.ai.document.service.DocumentService;
import com.exam.ai.document.service.DocumentVisionRecognitionClient;
import com.exam.ai.document.util.QuestionAnalysisParser;
import com.exam.ai.document.vo.AnalysisResponse;
import com.exam.ai.document.vo.AnalysisSummary;
import com.exam.ai.document.vo.ChunkProgressResponse;
import com.exam.ai.document.vo.DocumentResponse;
import com.exam.ai.document.vo.FailedPageResponse;
import com.exam.ai.document.vo.QuestionAnalysisResponse;
import com.exam.ai.question.entity.ExamQuestionSource;
import com.exam.ai.question.mapper.ExamQuestionSourceMapper;
import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.question.vo.QuestionResponse;
import com.exam.ai.system.service.NotificationService;
import com.exam.ai.system.service.SystemConfigService;
import com.exam.ai.util.CurrentUserUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * PDF 文档解析业务实现。
 *
 * <p>当前文档链路只支持 PDF：上传保存原文件，定时任务将 PDF 渲染为页图片，用户触发 AI
 * 后按页识别并把原始 JSON 保存到页 chunk。只有文档进入 AI_PARSE_COMPLETE 后，后处理定时任务
 * 才会统一读取 raw_json 并生成待确认题目。</p>
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final String PDF_TYPE = "pdf";
    private static final String PAGE_IMAGE_FORMAT = "png";
    private static final String PAGE_IMAGE_FILE_PATTERN = "page-%03d.png";
    private static final String PAGE_DIRECTORY = "pages";
    private static final int RENDER_DPI = 200;
    private static final int RENDER_BATCH_SIZE = 5;
    private static final int RAW_JSON_BATCH_SIZE = 5;
    private static final String PDF_NOT_READY_MESSAGE = "PDF 页面图片尚未准备完成，请稍后再试";
    private static final String DOCUMENT_RENDER_FAILED_TITLE = "PDF 页面渲染失败";
    private static final String DOCUMENT_ANALYSIS_FAILED_TITLE = "PDF AI 页面解析存在失败页";

    private final ExamDocumentMapper documentMapper;
    private final ExamDocumentAnalysisMapper analysisMapper;
    private final ExamDocumentAnalysisChunkMapper chunkMapper;
    private final ExamQuestionSourceMapper sourceMapper;
    private final QuestionBankService questionBankService;
    private final DocumentFileService fileService;
    private final DocumentVisionRecognitionClient visionRecognitionClient;
    private final DocumentQuestionAssemblerClient questionAssemblerClient;
    private final QuestionAnalysisParser parser;
    private final SystemConfigService systemConfigService;
    private final NotificationService notificationService;
    private final DocumentProperties documentProperties;
    private final String modelName;

    /**
     * 构造 PDF 文档解析服务。
     *
     * @param documentMapper 文档主表访问器。
     * @param analysisMapper 文档分析批次访问器。
     * @param chunkMapper PDF 页任务访问器。
     * @param sourceMapper 题目来源表访问器。
     * @param questionBankService 题库导入和查询服务。
     * @param fileService PDF 文件保存服务。
     * @param visionRecognitionClient OpenAI-compatible 页图片识别客户端。
     * @param questionAssemblerClient OpenAI-compatible 文档级题目合并客户端。
     * @param parser AI JSON 题目结果解析器。
     * @param systemConfigService 系统配置服务，用于读取单次页识别内部重试次数。
     * @param notificationService 站内通知服务。
     * @param documentProperties 文档存储配置。
     * @param modelName Spring AI OpenAI-compatible 模型名。
     */
    public DocumentServiceImpl(ExamDocumentMapper documentMapper,
                               ExamDocumentAnalysisMapper analysisMapper,
                               ExamDocumentAnalysisChunkMapper chunkMapper,
                               ExamQuestionSourceMapper sourceMapper,
                               QuestionBankService questionBankService,
                               DocumentFileService fileService,
                               DocumentVisionRecognitionClient visionRecognitionClient,
                               DocumentQuestionAssemblerClient questionAssemblerClient,
                               QuestionAnalysisParser parser,
                               SystemConfigService systemConfigService,
                               NotificationService notificationService,
                               DocumentProperties documentProperties,
                               @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String modelName) {
        this.documentMapper = documentMapper;
        this.analysisMapper = analysisMapper;
        this.chunkMapper = chunkMapper;
        this.sourceMapper = sourceMapper;
        this.questionBankService = questionBankService;
        this.fileService = fileService;
        this.visionRecognitionClient = visionRecognitionClient;
        this.questionAssemblerClient = questionAssemblerClient;
        this.parser = parser;
        this.systemConfigService = systemConfigService;
        this.notificationService = notificationService;
        this.documentProperties = documentProperties;
        this.modelName = modelName;
    }

    /**
     * 上传 PDF 文档并保存主记录。
     *
     * @param file 用户上传的 PDF 文件。
     * @return 文档基础信息。
     * @throws BusinessException 文件为空、大小超限、扩展名不是 PDF 或保存失败时抛出。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DocumentResponse upload(MultipartFile file) {
        DocumentFileService.StoredDocument stored = fileService.store(file);
        ExamDocument document = new ExamDocument();
        document.setOriginalFilename(stored.originalFilename());
        document.setStoredFilename(stored.storedFilename());
        document.setFileType(stored.fileType());
        document.setFileSize(stored.fileSize());
        document.setSha256(stored.sha256());
        document.setStoragePath(stored.storagePath());
        document.setPageCount(countPdfPages(Path.of(stored.storagePath())));
        document.setStatus(DocumentStatus.UPLOADED);
        documentMapper.insert(document);
        return toDocumentResponse(documentMapper.selectById(document.getId()), null);
    }

    /**
     * 分页查询当前用户上传的 PDF 文档。
     *
     * @param page 页码，从 1 开始。
     * @param size 每页数量。
     * @return 当前用户文档分页。
     */
    @Override
    public IPage<DocumentResponse> list(long page, long size) {
        LambdaQueryWrapper<ExamDocument> query = new LambdaQueryWrapper<ExamDocument>()
                .eq(ExamDocument::getCreateId, CurrentUserUtils.currentUserId())
                .orderByDesc(ExamDocument::getId);
        return documentMapper.selectPage(Page.of(page, size), query)
                .convert(document -> toDocumentResponse(document, latestSummary(document.getId())));
    }

    /**
     * 查询当前用户可见的文档详情。
     *
     * @param id 文档 ID。
     * @return 文档详情和最新分析摘要。
     * @throws BusinessException 文档不存在或无权访问时抛出。
     */
    @Override
    public DocumentResponse detail(Long id) {
        ExamDocument document = requireVisibleDocument(id);
        return toDocumentResponse(document, latestSummary(id));
    }

    /**
     * 获取当前用户可预览的 PDF 文件路径。
     *
     * @param id 文档 ID。
     * @return PDF 本地文件路径。
     * @throws BusinessException 文档不存在、无权访问、不是 PDF 或文件缺失时抛出。
     */
    @Override
    public Path pdfFile(Long id) {
        ExamDocument document = requireVisibleDocument(id);
        if (!PDF_TYPE.equalsIgnoreCase(document.getFileType())) {
            throw BusinessException.badRequest("仅支持预览 PDF 文件");
        }
        Path path = Path.of(document.getStoragePath()).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            throw BusinessException.badRequest("PDF 文件不存在");
        }
        return path;
    }

    /**
     * 按页执行 PDF AI 识别。
     *
     * <p>该方法只写页级 raw_json，不直接生成题目；文档进入 AI_PARSE_COMPLETE 后再由后处理定时任务
     * 统一处理 raw_json。页级识别不使用外层声明式事务，避免单页失败或后续状态刷新异常回滚已成功页结果。</p>
     *
     * @param documentId 文档 ID。
     * @return 最新分析状态和页进度。
     * @throws BusinessException 文档不存在、无权访问或页面图片未准备完成时抛出。
     */
    @Override
    public AnalysisResponse analyze(Long documentId) {
        ExamDocument document = requireVisibleDocument(documentId);
        ensureAnalyzable(document);
        ExamDocumentAnalysis analysis = requireLatestAnalysis(document.getId());
        List<ExamDocumentAnalysisChunk> chunks = chunks(analysis.getId());
        if (chunks.isEmpty()) {
            throw BusinessException.badRequest(PDF_NOT_READY_MESSAGE);
        }
        document.setStatus(DocumentStatus.PARSING);
        documentMapper.updateById(document);
        processPages(document, analysis, chunks.stream()
                .filter(chunk -> AnalysisChunkStatus.PENDING.equals(chunk.getStatus())
                        || AnalysisChunkStatus.FAILED.equals(chunk.getStatus()))
                .toList());
        refreshAfterPageRecognition(document, analysis);
        return toAnalysisResponse(analysisMapper.selectById(analysis.getId()));
    }

    /**
     * 查询最近一次文档分析结果。
     *
     * @param documentId 文档 ID。
     * @return 最近分析响应。
     * @throws BusinessException 文档不存在、无权访问或暂无分析记录时抛出。
     */
    @Override
    public AnalysisResponse latestAnalysis(Long documentId) {
        requireVisibleDocument(documentId);
        return toAnalysisResponse(requireLatestAnalysis(documentId));
    }

    /**
     * 查询文档 AI 解析失败页列表。
     *
     * @param documentId 文档 ID。
     * @return 失败页列表，按页码升序。
     */
    @Override
    public List<FailedPageResponse> failedPages(Long documentId) {
        ExamDocument document = requireVisibleDocument(documentId);
        ExamDocumentAnalysis analysis = requireLatestAnalysis(document.getId());
        return chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                        .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                        .eq(ExamDocumentAnalysisChunk::getStatus, AnalysisChunkStatus.FAILED)
                        .orderByAsc(ExamDocumentAnalysisChunk::getPageNo))
                .stream()
                .map(chunk -> new FailedPageResponse(chunk.getPageNo(), safeRetryCount(chunk),
                        chunk.getErrorMessage(), chunk.getPageImagePath(), chunk.getUpdateTime()))
                .toList();
    }

    /**
     * 批量重试用户选择的失败页。
     *
     * @param documentId 文档 ID。
     * @param request 失败页页码集合。
     * @return 最新分析状态。
     * @throws BusinessException 文档不存在、无权访问、状态不允许或页码非法时抛出。
     */
    @Override
    public AnalysisResponse retryFailedPages(Long documentId, RetryFailedPagesRequest request) {
        ExamDocument document = requireVisibleDocument(documentId);
        if (!DocumentStatus.AI_PARSE_FAILED_REVIEW.equals(document.getStatus())) {
            throw BusinessException.badRequest("当前文档没有待处理失败页");
        }
        ExamDocumentAnalysis analysis = requireLatestAnalysis(document.getId());
        Set<Integer> selectedPageNos = new HashSet<>(request.pageNos());
        List<ExamDocumentAnalysisChunk> selectedChunks = chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                .eq(ExamDocumentAnalysisChunk::getStatus, AnalysisChunkStatus.FAILED)
                .in(ExamDocumentAnalysisChunk::getPageNo, selectedPageNos)
                .orderByAsc(ExamDocumentAnalysisChunk::getPageNo));
        if (selectedChunks.size() != selectedPageNos.size()) {
            throw BusinessException.badRequest("存在不可重试的页码");
        }
        document.setStatus(DocumentStatus.PARSING);
        documentMapper.updateById(document);
        processPages(document, analysis, selectedChunks);
        refreshAfterPageRecognition(document, analysis);
        return toAnalysisResponse(analysisMapper.selectById(analysis.getId()));
    }

    /**
     * 确认跳过所有失败页，使文档进入 AI 解析完成状态。
     *
     * @param documentId 文档 ID。
     * @return 最新分析状态。
     * @throws BusinessException 文档不存在、无权访问、状态不允许或仍有待解析页时抛出。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AnalysisResponse confirmSkipFailedPages(Long documentId) {
        ExamDocument document = requireVisibleDocument(documentId);
        if (!DocumentStatus.AI_PARSE_FAILED_REVIEW.equals(document.getStatus())) {
            throw BusinessException.badRequest("当前文档没有待确认失败页");
        }
        ExamDocumentAnalysis analysis = requireLatestAnalysis(document.getId());
        ChunkProgressResponse progress = chunkProgress(analysis.getId());
        if (progress.pending() > 0 || progress.processing() > 0) {
            throw BusinessException.badRequest("仍有页面未完成解析，不能确认跳过");
        }
        List<ExamDocumentAnalysisChunk> failedChunks = chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                .eq(ExamDocumentAnalysisChunk::getStatus, AnalysisChunkStatus.FAILED));
        for (ExamDocumentAnalysisChunk chunk : failedChunks) {
            chunk.setStatus(AnalysisChunkStatus.SKIPPED);
            chunk.setFinishedAt(LocalDateTime.now());
            chunkMapper.updateById(chunk);
        }
        document.setStatus(DocumentStatus.AI_PARSE_COMPLETE);
        documentMapper.updateById(document);
        analysis.setStatus(AnalysisStatus.SUCCESS);
        analysis.setErrorMessage(null);
        analysisMapper.updateById(analysis);
        return toAnalysisResponse(analysisMapper.selectById(analysis.getId()));
    }

    /**
     * 定时任务渲染待处理 PDF 的页面图片。
     */
    @Override
    public void renderPendingPdfPages() {
        List<ExamDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<ExamDocument>()
                .in(ExamDocument::getStatus, DocumentStatus.UPLOADED, DocumentStatus.PAGE_RENDER_FAILED)
                .eq(ExamDocument::getFileType, PDF_TYPE)
                .orderByAsc(ExamDocument::getId)
                .last("LIMIT " + RENDER_BATCH_SIZE));
        for (ExamDocument document : documents) {
            renderDocumentPages(document);
        }
    }

    /**
     * 定时任务处理已完成页级 AI 解析的 raw_json。
     */
    @Override
    public void processCompletedRawJson() {
        List<ExamDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<ExamDocument>()
                .eq(ExamDocument::getStatus, DocumentStatus.AI_PARSE_COMPLETE)
                .orderByAsc(ExamDocument::getId)
                .last("LIMIT " + RAW_JSON_BATCH_SIZE));
        for (ExamDocument document : documents) {
            processDocumentRawJson(document);
        }
    }

    /**
     * 渲染单个 PDF 文档页面并生成页 chunk。
     *
     * @param document 待渲染 PDF 文档。
     */
    private void renderDocumentPages(ExamDocument document) {
        document.setStatus(DocumentStatus.PAGE_RENDERING);
        documentMapper.updateById(document);
        try {
            ExamDocumentAnalysis analysis = ensureAnalysis(document);
            Path pdfPath = Path.of(document.getStoragePath()).toAbsolutePath().normalize();
            Path pageDirectory = pageDirectory(document.getId());
            Files.createDirectories(pageDirectory);
            try (PDDocument pdf = Loader.loadPDF(pdfPath.toFile())) {
                PDFRenderer renderer = new PDFRenderer(pdf);
                for (int pageIndex = 0; pageIndex < pdf.getNumberOfPages(); pageIndex++) {
                    int pageNo = pageIndex + 1;
                    BufferedImage image = renderer.renderImageWithDPI(pageIndex, RENDER_DPI, ImageType.RGB);
                    Path imagePath = pageDirectory.resolve(PAGE_IMAGE_FILE_PATTERN.formatted(pageNo));
                    ImageIO.write(image, PAGE_IMAGE_FORMAT, imagePath.toFile());
                    upsertPageChunk(document, analysis, pageNo, imagePath, image.getWidth(), image.getHeight());
                }
            }
            document.setStatus(DocumentStatus.PAGE_READY);
            documentMapper.updateById(document);
        } catch (Exception ex) {
            document.setStatus(DocumentStatus.PAGE_RENDER_FAILED);
            documentMapper.updateById(document);
            notificationService.create(document.getCreateId(), DOCUMENT_RENDER_FAILED_TITLE,
                    "文档《" + document.getOriginalFilename() + "》PDF 页面渲染失败：" + ex.getMessage(),
                    NotificationService.TYPE_AI_DOCUMENT_RENDER_FAILED, NotificationService.BUSINESS_DOCUMENT, document.getId());
        }
    }

    /**
     * 新增或更新 PDF 页 chunk。
     *
     * @param document 文档实体。
     * @param analysis 分析批次。
     * @param pageNo 页码。
     * @param imagePath 页图片路径。
     * @param width 图片宽度。
     * @param height 图片高度。
     */
    private void upsertPageChunk(ExamDocument document, ExamDocumentAnalysis analysis, int pageNo, Path imagePath, int width, int height) {
        ExamDocumentAnalysisChunk chunk = chunkMapper.selectOne(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                .eq(ExamDocumentAnalysisChunk::getPageNo, pageNo)
                .last("LIMIT 1"));
        if (chunk == null) {
            chunk = new ExamDocumentAnalysisChunk();
            chunk.setAnalysisId(analysis.getId());
            chunk.setDocumentId(document.getId());
            chunk.setPageNo(pageNo);
            chunk.setRetryCount(0);
            chunk.setStatus(AnalysisChunkStatus.PENDING);
            chunk.setCreateId(document.getCreateId());
        }
        chunk.setPageImagePath(imagePath.toString());
        chunk.setPageWidth(width);
        chunk.setPageHeight(height);
        if (!AnalysisChunkStatus.SUCCESS.equals(chunk.getStatus())) {
            chunk.setStatus(AnalysisChunkStatus.PENDING);
        }
        if (chunk.getId() == null) {
            chunkMapper.insert(chunk);
        } else {
            chunkMapper.updateById(chunk);
        }
    }

    /**
     * 顺序处理页级 AI 识别任务。
     *
     * @param document 文档实体。
     * @param analysis 分析批次。
     * @param pages 待处理页集合。
     */
    private void processPages(ExamDocument document, ExamDocumentAnalysis analysis, List<ExamDocumentAnalysisChunk> pages) {
        for (ExamDocumentAnalysisChunk page : pages) {
            processSinglePage(page);
        }
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysisMapper.updateById(analysis);
    }

    /**
     * 识别单页图片并保存 raw_json。
     *
     * @param page 页级任务。
     */
    private void processSinglePage(ExamDocumentAnalysisChunk page) {
        int maxAttempts = 1 + Math.max(0, systemConfigService.aiDocumentAnalysisMaxRetries());
        int failedAttempts = 0;
        String latestError = null;
        page.setStatus(AnalysisChunkStatus.PROCESSING);
        page.setStartedAt(LocalDateTime.now());
        page.setFinishedAt(null);
        page.setErrorMessage(null);
        chunkMapper.updateById(page);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String rawJson = visionRecognitionClient.recognizePage(Path.of(page.getPageImagePath()), page.getPageNo());
                page.setRawJson(rawJson);
                page.setStatus(AnalysisChunkStatus.SUCCESS);
                page.setErrorMessage(null);
                page.setFinishedAt(LocalDateTime.now());
                page.setRetryCount(safeRetryCount(page) + failedAttempts);
                page.setNotifiedAt(null);
                chunkMapper.updateById(page);
                return;
            } catch (Exception ex) {
                failedAttempts++;
                latestError = ex.getMessage();
            }
        }
        page.setStatus(AnalysisChunkStatus.FAILED);
        page.setErrorMessage(latestError);
        page.setFinishedAt(LocalDateTime.now());
        page.setRetryCount(safeRetryCount(page) + failedAttempts);
        chunkMapper.updateById(page);
    }

    /**
     * 根据页级识别结果刷新文档与分析批次状态。
     *
     * @param document 文档实体。
     * @param analysis 分析批次。
     */
    private void refreshAfterPageRecognition(ExamDocument document, ExamDocumentAnalysis analysis) {
        ChunkProgressResponse progress = chunkProgress(analysis.getId());
        if (progress.failed() > 0) {
            document.setStatus(DocumentStatus.AI_PARSE_FAILED_REVIEW);
            analysis.setStatus(progress.success() > 0 ? AnalysisStatus.PARTIAL_FAILED : AnalysisStatus.FAILED);
            analysis.setErrorMessage(progress.latestErrorMessage());
            notifyFailedPages(document, analysis);
        } else if (progress.pending() == 0 && progress.processing() == 0 && progress.total() > 0) {
            document.setStatus(DocumentStatus.AI_PARSE_COMPLETE);
            analysis.setStatus(AnalysisStatus.SUCCESS);
            analysis.setErrorMessage(null);
        } else {
            document.setStatus(DocumentStatus.PAGE_READY);
        }
        documentMapper.updateById(document);
        analysisMapper.updateById(analysis);
    }

    /**
     * 通知文档创建人进入失败页确认页面。
     *
     * @param document 文档实体。
     * @param analysis 分析批次。
     */
    private void notifyFailedPages(ExamDocument document, ExamDocumentAnalysis analysis) {
        List<ExamDocumentAnalysisChunk> failedChunks = chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                .eq(ExamDocumentAnalysisChunk::getStatus, AnalysisChunkStatus.FAILED)
                .isNull(ExamDocumentAnalysisChunk::getNotifiedAt));
        if (failedChunks.isEmpty()) {
            return;
        }
        notificationService.create(document.getCreateId(), DOCUMENT_ANALYSIS_FAILED_TITLE,
                "文档《" + document.getOriginalFilename() + "》存在 " + failedChunks.size() + " 个失败页，请在文档详情中重试或确认跳过。",
                NotificationService.TYPE_AI_DOCUMENT_ANALYSIS_FAILED, NotificationService.BUSINESS_DOCUMENT, document.getId());
        LocalDateTime notifiedAt = LocalDateTime.now();
        for (ExamDocumentAnalysisChunk chunk : failedChunks) {
            chunk.setNotifiedAt(notifiedAt);
            chunkMapper.updateById(chunk);
        }
    }

    /**
     * 统一处理单个 AI_PARSE_COMPLETE 文档的 raw_json。
     *
     * @param document 文档实体。
     */
    private void processDocumentRawJson(ExamDocument document) {
        ExamDocumentAnalysis analysis = latestAnalysisEntity(document.getId());
        if (analysis == null) {
            return;
        }
        document.setStatus(DocumentStatus.RAW_JSON_PROCESSING);
        documentMapper.updateById(document);
        try {
            List<ExamDocumentAnalysisChunk> successPages = chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                    .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                    .eq(ExamDocumentAnalysisChunk::getStatus, AnalysisChunkStatus.SUCCESS)
                    .orderByAsc(ExamDocumentAnalysisChunk::getPageNo));
            List<AiPageAnalysisResult> pageAnalyses = new ArrayList<>();
            for (ExamDocumentAnalysisChunk page : successPages) {
                pageAnalyses.add(parser.parsePageAnalysis(page.getRawJson()));
            }
            String assembledRawJson = pageAnalyses.isEmpty() ? "{\"questions\":[]}" : questionAssemblerClient.assembleQuestions(pageAnalyses);
            AiDocumentAssembleResult assembleResult = parser.parseAssembleResult(assembledRawJson);
            importAssembledQuestions(document, analysis, successPages, assembleResult.questions());
            document.setStatus(DocumentStatus.PENDING_CONFIRMATION);
            documentMapper.updateById(document);
            analysis.setStatus(AnalysisStatus.SUCCESS);
            analysis.setRawJson(assembledRawJson);
            analysis.setErrorMessage(null);
            analysisMapper.updateById(analysis);
        } catch (Exception ex) {
            document.setStatus(DocumentStatus.PARSE_FAILED);
            documentMapper.updateById(document);
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(ex.getMessage());
            analysisMapper.updateById(analysis);
            notificationService.create(document.getCreateId(), "PDF AI 结果处理失败",
                    "文档《" + document.getOriginalFilename() + "》AI 结果处理失败：" + ex.getMessage(),
                    NotificationService.TYPE_AI_DOCUMENT_ANALYSIS_FAILED, NotificationService.BUSINESS_DOCUMENT, document.getId());
        }
    }

    /**
     * 将文档级合并后的完整题目导入待确认题库。
     *
     * @param document 文档实体。
     * @param analysis 分析批次。
     * @param successPages 成功完成页级识别的页面任务。
     * @param questions 文档级合并后的完整题目集合。
     * @throws JsonProcessingException 题目选项序列化失败时抛出。
     */
    private void importAssembledQuestions(ExamDocument document, ExamDocumentAnalysis analysis,
                                          List<ExamDocumentAnalysisChunk> successPages,
                                          List<AiAssembledQuestionItem> questions)
            throws JsonProcessingException {
        if (questions == null || questions.isEmpty()) {
            return;
        }
        Map<Integer, Long> chunkIdByPageNo = successPages.stream()
                .collect(Collectors.toMap(ExamDocumentAnalysisChunk::getPageNo, ExamDocumentAnalysisChunk::getId, (left, right) -> left));
        int sortOrder = 1;
        for (AiAssembledQuestionItem question : questions) {
            Long primaryChunkId = primaryChunkId(question.sourcePageNos(), chunkIdByPageNo);
            questionBankService.importQuestion(toQuestionItem(question), document.getId(), analysis.getId(),
                    primaryChunkId, question.sourcePageNos(), sortOrder++, document.getCreateId());
        }
    }

    /**
     * 获取跨页题目的主分片 ID，默认使用来源页码中的第一页。
     *
     * @param sourcePageNos 来源页码集合。
     * @param chunkIdByPageNo 页码到 chunk ID 的映射。
     * @return 主 chunk ID；无法匹配时返回 {@code null}。
     */
    private Long primaryChunkId(List<Integer> sourcePageNos, Map<Integer, Long> chunkIdByPageNo) {
        if (sourcePageNos == null || sourcePageNos.isEmpty()) {
            return null;
        }
        return sourcePageNos.stream()
                .filter(chunkIdByPageNo::containsKey)
                .sorted()
                .map(chunkIdByPageNo::get)
                .findFirst()
                .orElse(null);
    }

    /**
     * 将文档级合并题目转换为现有题库入库 DTO。
     *
     * @param question 文档级合并题目。
     * @return 题库入库 DTO。
     */
    private AiQuestionItem toQuestionItem(AiAssembledQuestionItem question) {
        return new AiQuestionItem(
                question.type(),
                question.stem(),
                question.options(),
                question.standardAnswer(),
                question.explanation(),
                question.difficultyStars(),
                question.confidence(),
                question.categoryName()
        );
    }

    /**
     * 校验文档当前状态是否允许发起页级 AI 识别。
     *
     * @param document 文档实体。
     */
    private void ensureAnalyzable(ExamDocument document) {
        if (DocumentStatus.PAGE_RENDERING.equals(document.getStatus())) {
            throw BusinessException.badRequest("PDF 页面图片正在生成，请稍后再试");
        }
        if (DocumentStatus.UPLOADED.equals(document.getStatus()) || DocumentStatus.PAGE_RENDER_FAILED.equals(document.getStatus())) {
            throw BusinessException.badRequest(PDF_NOT_READY_MESSAGE);
        }
        if (!List.of(DocumentStatus.PAGE_READY, DocumentStatus.AI_PARSE_FAILED_REVIEW).contains(document.getStatus())) {
            throw BusinessException.badRequest("当前文档状态不允许 AI 解析");
        }
    }

    /**
     * 查询当前用户可见文档。
     *
     * @param id 文档 ID。
     * @return 文档实体。
     */
    private ExamDocument requireVisibleDocument(Long id) {
        ExamDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw BusinessException.badRequest("文档不存在");
        }
        if (!CurrentUserUtils.currentUserId().equals(document.getCreateId())) {
            throw BusinessException.forbidden();
        }
        return document;
    }

    /**
     * 查询或创建文档分析批次。
     *
     * @param document 文档实体。
     * @return 分析批次。
     */
    private ExamDocumentAnalysis ensureAnalysis(ExamDocument document) {
        ExamDocumentAnalysis analysis = latestAnalysisEntity(document.getId());
        if (analysis != null) {
            return analysis;
        }
        analysis = new ExamDocumentAnalysis();
        analysis.setDocumentId(document.getId());
        analysis.setModelName(modelName);
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setCreateId(document.getCreateId());
        analysisMapper.insert(analysis);
        return analysis;
    }

    /**
     * 查询最近分析批次，不存在时抛出业务异常。
     *
     * @param documentId 文档 ID。
     * @return 最近分析批次。
     */
    private ExamDocumentAnalysis requireLatestAnalysis(Long documentId) {
        ExamDocumentAnalysis analysis = latestAnalysisEntity(documentId);
        if (analysis == null) {
            throw BusinessException.badRequest("暂无分析记录");
        }
        return analysis;
    }

    /**
     * 查询最近分析批次。
     *
     * @param documentId 文档 ID。
     * @return 最近分析批次；不存在返回 {@code null}。
     */
    private ExamDocumentAnalysis latestAnalysisEntity(Long documentId) {
        return analysisMapper.selectOne(new LambdaQueryWrapper<ExamDocumentAnalysis>()
                .eq(ExamDocumentAnalysis::getDocumentId, documentId)
                .orderByDesc(ExamDocumentAnalysis::getId)
                .last("LIMIT 1"));
    }

    /**
     * 查询分析批次下所有页 chunk。
     *
     * @param analysisId 分析批次 ID。
     * @return 页 chunk 列表。
     */
    private List<ExamDocumentAnalysisChunk> chunks(Long analysisId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysisId)
                .orderByAsc(ExamDocumentAnalysisChunk::getPageNo));
    }

    /**
     * 统计页级解析进度。
     *
     * @param analysisId 分析批次 ID。
     * @return 进度响应。
     */
    private ChunkProgressResponse chunkProgress(Long analysisId) {
        List<ExamDocumentAnalysisChunk> chunks = chunks(analysisId);
        int success = 0;
        int failed = 0;
        int pending = 0;
        int processing = 0;
        int skipped = 0;
        String latestError = null;
        for (ExamDocumentAnalysisChunk chunk : chunks) {
            if (AnalysisChunkStatus.SUCCESS.equals(chunk.getStatus())) {
                success++;
            } else if (AnalysisChunkStatus.FAILED.equals(chunk.getStatus())) {
                failed++;
                latestError = chunk.getErrorMessage();
            } else if (AnalysisChunkStatus.PROCESSING.equals(chunk.getStatus())) {
                processing++;
            } else if (AnalysisChunkStatus.SKIPPED.equals(chunk.getStatus())) {
                skipped++;
            } else {
                pending++;
            }
        }
        return new ChunkProgressResponse(chunks.size(), success, failed, pending, processing, skipped, latestError);
    }

    /**
     * 转换文档实体为前端响应。
     *
     * @param document 文档实体。
     * @param latestAnalysis 最新分析摘要。
     * @return 文档响应。
     */
    private DocumentResponse toDocumentResponse(ExamDocument document, AnalysisSummary latestAnalysis) {
        int renderedPageCount = renderedPageCount(document.getId());
        int pageCount = effectivePageCount(document, renderedPageCount);
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getSha256(),
                pageCount,
                renderedPageCount,
                renderProgressPercent(document.getStatus(), pageCount, renderedPageCount),
                document.getStatus(),
                document.getCreateId(),
                document.getCreateTime(),
                latestAnalysis
        );
    }

    /**
     * 读取 PDF 总页数，用于上传响应和后续分片真实进度计算。
     *
     * @param pdfPath 已落盘的 PDF 文件路径。
     * @return PDF 总页数。
     */
    private int countPdfPages(Path pdfPath) {
        try (PDDocument pdf = Loader.loadPDF(pdfPath.toFile())) {
            return pdf.getNumberOfPages();
        } catch (IOException ex) {
            throw BusinessException.badRequest("PDF 页数读取失败");
        }
    }

    /**
     * 统计当前文档已经生成的页图片分片数量。
     *
     * @param documentId 文档 ID。
     * @return 已生成页分片数量。
     */
    private int renderedPageCount(Long documentId) {
        Long count = chunkMapper.selectCount(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getDocumentId, documentId));
        return count == null ? 0 : count.intValue();
    }

    /**
     * 获取可用于进度展示的总页数，兼容历史数据缺少 page_count 的情况。
     *
     * @param document 文档实体。
     * @param renderedPageCount 已生成页数。
     * @return 有效总页数。
     */
    private int effectivePageCount(ExamDocument document, int renderedPageCount) {
        if (document.getPageCount() != null && document.getPageCount() > 0) {
            return document.getPageCount();
        }
        return renderedPageCount;
    }

    /**
     * 根据状态、总页数和已生成页数计算页图片分片百分比。
     *
     * @param status 文档状态。
     * @param pageCount PDF 总页数。
     * @param renderedPageCount 已生成页数。
     * @return 0 到 100 的整数进度。
     */
    private int renderProgressPercent(String status, int pageCount, int renderedPageCount) {
        if (DocumentStatus.PAGE_READY.equals(status)) {
            return 100;
        }
        if (DocumentStatus.UPLOADED.equals(status)) {
            return 0;
        }
        if (pageCount <= 0) {
            return 0;
        }
        return Math.min(100, Math.max(0, renderedPageCount * 100 / pageCount));
    }

    /**
     * 查询文档最新分析摘要。
     *
     * @param documentId 文档 ID。
     * @return 最新分析摘要；不存在时返回 {@code null}。
     */
    private AnalysisSummary latestSummary(Long documentId) {
        ExamDocumentAnalysis analysis = latestAnalysisEntity(documentId);
        if (analysis == null) {
            return null;
        }
        Long count = sourceMapper.selectCount(new LambdaQueryWrapper<ExamQuestionSource>()
                .eq(ExamQuestionSource::getAnalysisId, analysis.getId()));
        return new AnalysisSummary(analysis.getId(), analysis.getStatus(), analysis.getModelName(),
                count == null ? 0 : count.intValue(), analysis.getErrorMessage(), chunkProgress(analysis.getId()), analysis.getCreateTime());
    }

    /**
     * 转换分析批次为前端响应。
     *
     * @param analysis 分析批次。
     * @return 分析响应。
     */
    private AnalysisResponse toAnalysisResponse(ExamDocumentAnalysis analysis) {
        List<ExamQuestionSource> sources = sourceMapper.selectList(new LambdaQueryWrapper<ExamQuestionSource>()
                .eq(ExamQuestionSource::getAnalysisId, analysis.getId())
                .orderByAsc(ExamQuestionSource::getSortOrder));
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getDocumentId(),
                analysis.getStatus(),
                analysis.getModelName(),
                analysis.getErrorMessage(),
                chunkProgress(analysis.getId()),
                analysis.getCreateTime(),
                sources.stream().map(this::toQuestionResponse).toList()
        );
    }

    /**
     * 转换题目来源为页面展示题目。
     *
     * @param source 题目来源。
     * @return 分析题目响应。
     */
    private QuestionAnalysisResponse toQuestionResponse(ExamQuestionSource source) {
        QuestionResponse question = questionBankService.detail(source.getQuestionId());
        return new QuestionAnalysisResponse(
                question.id(),
                question.categoryId(),
                question.categoryName(),
                question.questionType(),
                question.stem(),
                question.options(),
                question.standardAnswer(),
                question.explanation(),
                question.difficultyStars(),
                question.state(),
                false,
                source.getConfidence(),
                source.getSortOrder()
        );
    }

    /**
     * 获取页重试次数，空值按 0 处理。
     *
     * @param chunk 页 chunk。
     * @return 安全重试次数。
     */
    private int safeRetryCount(ExamDocumentAnalysisChunk chunk) {
        return chunk.getRetryCount() == null ? 0 : chunk.getRetryCount();
    }

    /**
     * 获取文档页图片目录。
     *
     * @param documentId 文档 ID。
     * @return 页图片目录。
     */
    private Path pageDirectory(Long documentId) {
        return documentProperties.getStoragePath().toAbsolutePath().normalize()
                .resolve(String.valueOf(documentId))
                .resolve(PAGE_DIRECTORY)
                .normalize();
    }

}
