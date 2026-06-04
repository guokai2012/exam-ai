package com.exam.ai.document.service.impl;

import com.exam.ai.document.service.DocumentService;
import com.exam.ai.document.service.DocumentFileService;
import com.exam.ai.document.util.DocumentChunker;
import com.exam.ai.document.util.QuestionAnalysisParser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.common.config.AiAnalysisProperties;
import com.exam.ai.document.entity.AnalysisChunkStatus;
import com.exam.ai.document.entity.AnalysisStatus;
import com.exam.ai.document.entity.DocumentStatus;
import com.exam.ai.document.entity.ExamDocument;
import com.exam.ai.document.entity.ExamDocumentAnalysis;
import com.exam.ai.document.entity.ExamDocumentAnalysisChunk;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.document.dto.AiQuestionResult;
import com.exam.ai.document.vo.AnalysisResponse;
import com.exam.ai.document.vo.AnalysisSummary;
import com.exam.ai.document.vo.ChunkProgressResponse;
import com.exam.ai.document.vo.DocumentContentResponse;
import com.exam.ai.document.vo.DocumentResponse;
import com.exam.ai.document.vo.QuestionAnalysisResponse;
import com.exam.ai.document.mapper.ExamDocumentAnalysisChunkMapper;
import com.exam.ai.document.mapper.ExamDocumentAnalysisMapper;
import com.exam.ai.document.mapper.ExamDocumentMapper;
import com.exam.ai.question.entity.ExamQuestionSource;
import com.exam.ai.question.dto.QuestionImportResult;
import com.exam.ai.question.vo.QuestionResponse;
import com.exam.ai.question.mapper.ExamQuestionSourceMapper;
import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.system.service.SystemConfigService;
import com.exam.ai.util.CurrentUserUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析业务实现类，负责文档上传、文本提取结果入库、AI 分片解析和解析结果查询。
 *
 * <p>解析流程以文档、解析批次、解析分片三层数据承载状态，失败分片可重复进入解析流程，
 * 便于在 AI 临时失败或配置重试次数变化后继续处理。</p>
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final String SYSTEM_PROMPT = """
            你是考试题目结构化解析助手。请从用户提供的文档文本中识别已有题目，不要生成新题。
            只返回 JSON，不要返回 Markdown、解释文字或代码块。
            JSON 格式必须为：
            {"questions":[{"type":"SINGLE_CHOICE|MULTIPLE_CHOICE|TRUE_FALSE|SHORT_ANSWER","stem":"题干","options":["A. ..."],"standardAnswer":"标准答案","explanation":"答案解析","difficultyStars":1,"confidence":0.9,"categoryName":"题库分类"}]}
            difficultyStars 必须是 1 到 5 的整数，confidence 必须是 0 到 1。
            如果某题没有选项，options 返回空数组。
            categoryName 由题目知识点或题目类型归纳得出，例如 Java 基础、数学应用题、鸡兔同笼、数据库索引。
            """;

    private final ExamDocumentMapper documentMapper;
    private final ExamDocumentAnalysisMapper analysisMapper;
    private final ExamDocumentAnalysisChunkMapper chunkMapper;
    private final ExamQuestionSourceMapper sourceMapper;
    private final QuestionBankService questionBankService;
    private final DocumentFileService fileService;
    private final DocumentChunker documentChunker;
    private final QuestionAnalysisParser parser;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;
    private final AiAnalysisProperties aiProperties;
    private final SystemConfigService systemConfigService;
    private final String modelName;
    private final String apiKey;

    /**
     * 构造 DocumentServiceImpl 实例并注入运行所需依赖。
     * @param documentMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysisMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param chunkMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param sourceMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param questionBankService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param fileService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param documentChunker 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param parser 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param chatClientBuilderProvider 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param objectMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param aiProperties 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param systemConfigService 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param modelName 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param apiKey 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentServiceImpl(ExamDocumentMapper documentMapper, ExamDocumentAnalysisMapper analysisMapper,
                           ExamDocumentAnalysisChunkMapper chunkMapper, ExamQuestionSourceMapper sourceMapper,
                           QuestionBankService questionBankService, DocumentFileService fileService,
                           DocumentChunker documentChunker, QuestionAnalysisParser parser,
                           ObjectProvider<ChatClient.Builder> chatClientBuilderProvider, ObjectMapper objectMapper,
                           AiAnalysisProperties aiProperties, SystemConfigService systemConfigService,
                           @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String modelName,
                           @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.documentMapper = documentMapper;
        this.analysisMapper = analysisMapper;
        this.chunkMapper = chunkMapper;
        this.sourceMapper = sourceMapper;
        this.questionBankService = questionBankService;
        this.fileService = fileService;
        this.documentChunker = documentChunker;
        this.parser = parser;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.systemConfigService = systemConfigService;
        this.modelName = modelName;
        this.apiKey = apiKey;
    }

    /**
     * 上传文档并保存文件元数据、提取文本和上传人信息。
     * @param file 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public DocumentResponse upload(MultipartFile file) {
        DocumentFileService.StoredDocument stored = fileService.store(file);
        // 文件服务已完成类型、大小、落盘和文本提取校验，这里只负责业务元数据入库。
        ExamDocument document = new ExamDocument();
        document.setOriginalFilename(stored.originalFilename());
        document.setStoredFilename(stored.storedFilename());
        document.setFileType(stored.fileType());
        document.setFileSize(stored.fileSize());
        document.setSha256(stored.sha256());
        document.setStoragePath(stored.storagePath());
        document.setExtractedText(stored.extractedText());
        document.setStatus(DocumentStatus.UPLOADED);
        documentMapper.insert(document);
        return toDocumentResponse(documentMapper.selectById(document.getId()), null);
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<DocumentResponse> list(long page, long size) {
        LambdaQueryWrapper<ExamDocument> query = new LambdaQueryWrapper<ExamDocument>()
                .eq(ExamDocument::getCreateId, CurrentUserUtils.currentUserId())
                .orderByDesc(ExamDocument::getId);
        return documentMapper.selectPage(Page.of(page, size), query)
                .convert(document -> toDocumentResponse(document, latestSummary(document.getId())));
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentResponse detail(Long id) {
        ExamDocument document = requireVisibleDocument(id);
        return toDocumentResponse(document, latestSummary(id));
    }

    /**
     * 启动文档 AI 解析，将文档文本拆分为可重试分片并汇总最终解析状态。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public DocumentContentResponse content(Long id) {
        ExamDocument document = requireVisibleDocument(id);
        return new DocumentContentResponse(id, document.getExtractedText());
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public AnalysisResponse analyze(Long documentId) {
        Long currentUserId = CurrentUserUtils.currentUserId();
        ExamDocument document = requireVisibleDocument(documentId);
        if (!List.of(DocumentStatus.UPLOADED, DocumentStatus.PARSE_FAILED, DocumentStatus.PARSE_PARTIAL_FAILED).contains(document.getStatus())) {
            throw BusinessException.badRequest("当前文档状态不允许 AI 解析");
        }
        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            throw BusinessException.badRequest("文档没有可分析的提取文本");
        }
        document.setStatus(DocumentStatus.PARSING);
        documentMapper.updateById(document);
        // 已存在失败或部分失败批次时复用原分片，避免重复创建来源记录。
        ExamDocumentAnalysis analysis = analysisForParsing(document, currentUserId);
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setErrorMessage(null);
        analysisMapper.updateById(analysis);
        processPendingChunks(document, analysis, currentUserId);
        finishAnalysis(document, analysis);
        return toAnalysisResponse(analysisMapper.selectById(analysis.getId()));
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AnalysisResponse latestAnalysis(Long documentId) {
        requireVisibleDocument(documentId);
        ExamDocumentAnalysis analysis = latestAnalysisEntity(documentId);
        if (analysis == null) {
            throw BusinessException.badRequest("暂无分析结果");
        }
        return toAnalysisResponse(analysis);
    }

    /**
     * 获取本次解析批次；新文档创建新批次，失败重试优先复用已有分片。
     * @param document 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private ExamDocumentAnalysis analysisForParsing(ExamDocument document, Long userId) {
        ExamDocumentAnalysis analysis = DocumentStatus.UPLOADED.equals(document.getStatus())
                ? null
                : latestAnalysisEntity(document.getId());
        if (analysis == null || chunks(analysis.getId()).isEmpty()) {
            analysis = newAnalysis(document.getId(), userId);
            analysis.setStatus(AnalysisStatus.PROCESSING);
            analysisMapper.insert(analysis);
            // 分片记录持久化后，每个分片可单独记录成功、失败、重试次数和原始 AI 响应。
            createChunks(document, analysis.getId());
        }
        return analysis;
    }

    /**
     * 根据最大输入字符数创建解析分片记录。
     * @param document 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysisId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void createChunks(ExamDocument document, Long analysisId) {
        List<DocumentChunker.DocumentChunk> chunks = documentChunker.chunk(document.getExtractedText(), aiProperties.getMaxInputChars());
        for (DocumentChunker.DocumentChunk chunk : chunks) {
            // 分片哈希和偏移量用于排查 AI 解析问题，也方便后续做断点重试。
            ExamDocumentAnalysisChunk entity = new ExamDocumentAnalysisChunk();
            entity.setAnalysisId(analysisId);
            entity.setDocumentId(document.getId());
            entity.setChunkIndex(chunk.chunkIndex());
            entity.setChunkText(chunk.chunkText());
            entity.setChunkHash(chunk.chunkHash());
            entity.setStartOffset(chunk.startOffset());
            entity.setEndOffset(chunk.endOffset());
            entity.setQuestionCountEstimate(chunk.questionCountEstimate());
            entity.setOversized(chunk.oversized());
            entity.setStatus(AnalysisChunkStatus.PENDING);
            entity.setRetryCount(0);
            chunkMapper.insert(entity);
        }
    }

    /**
     * 顺序处理待解析分片，保证题目来源排序与原文片段顺序一致。
     * @param document 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysis 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void processPendingChunks(ExamDocument document, ExamDocumentAnalysis analysis, Long userId) {
        List<ExamDocumentAnalysisChunk> pendingChunks = chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                .in(ExamDocumentAnalysisChunk::getStatus, AnalysisChunkStatus.PENDING, AnalysisChunkStatus.FAILED, AnalysisChunkStatus.PROCESSING)
                .orderByAsc(ExamDocumentAnalysisChunk::getChunkIndex));
        for (ExamDocumentAnalysisChunk chunk : pendingChunks) {
            // 分片内部捕获异常并写入状态，避免单个片段失败中断整个批次汇总。
            processChunk(document, analysis, chunk, userId);
        }
    }

    /**
     * 解析单个文档分片并将 AI 结果导入题库。
     * @param document 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysis 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param chunk 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void processChunk(ExamDocument document, ExamDocumentAnalysis analysis, ExamDocumentAnalysisChunk chunk, Long userId) {
        chunk.setStatus(AnalysisChunkStatus.PROCESSING);
        chunk.setStartedAt(LocalDateTime.now());
        chunk.setFinishedAt(null);
        chunk.setErrorMessage(null);
        chunkMapper.updateById(chunk);
        try {
            AnalysisComputation computation = analyzeChunkWithRetry(chunk.getChunkText());
            parser.validate(new AiQuestionResult(computation.items()));
            // 题目入库统一交给题库服务处理，确保分类、去重和题目状态规则只有一处实现。
            List<QuestionImportResult> results = saveQuestions(document.getId(), analysis.getId(), chunk.getId(),
                    chunkSortOrderBase(chunk), userId, computation.items());
            chunk.setStatus(AnalysisChunkStatus.SUCCESS);
            chunk.setRawJson(computation.rawJson());
            chunk.setErrorMessage(null);
            chunk.setFinishedAt(LocalDateTime.now());
            chunkMapper.updateById(chunk);
            if (!results.isEmpty()) {
                analysis.setRawJson(appendRawJson(analysis.getRawJson(), computation.rawJson()));
                analysisMapper.updateById(analysis);
            }
        } catch (Exception ex) {
            // 失败分片保留错误和原始响应，下一次解析可基于重试次数继续处理。
            chunk.setStatus(AnalysisChunkStatus.FAILED);
            chunk.setRetryCount(safeRetryCount(chunk) + 1);
            chunk.setErrorMessage(ex.getMessage());
            if (ex instanceof AiAnalysisException aiException) {
                chunk.setRawJson(aiException.rawResponse());
            }
            chunk.setFinishedAt(LocalDateTime.now());
            chunkMapper.updateById(chunk);
        }
    }

    /**
     * 按系统配置的最大重试次数调用 AI，最终保留最后一次异常。
     * @param text 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private AnalysisComputation analyzeChunkWithRetry(String text) throws JsonProcessingException {
        int attempts = 1 + Math.max(0, systemConfigService.aiDocumentAnalysisMaxRetries());
        Exception lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return analyzeChunk(text);
            } catch (Exception ex) {
                // 这里不中断循环，让临时网络或模型响应异常有机会通过配置重试恢复。
                lastException = ex;
            }
        }
        if (lastException instanceof JsonProcessingException jsonProcessingException) {
            throw jsonProcessingException;
        }
        if (lastException instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw BusinessException.badRequest("AI 分析失败");
    }

    /**
     * 调用 AI 模型解析文档片段，并将模型响应转换为结构化题目集合。
     * @param text 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private AnalysisComputation analyzeChunk(String text) throws JsonProcessingException {
        if (apiKey == null || apiKey.isBlank() || "sk-placeholder".equals(apiKey)) {
            throw BusinessException.badRequest("AI API Key 未配置");
        }
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw BusinessException.badRequest("AI 客户端未配置");
        }
        String raw = builder.build().prompt()
                .system(SYSTEM_PROMPT)
                .user("请解析以下文档片段中的完整题目：\n\n" + text)
                .call()
                .content();
        try {
            // 解析器只接受 JSON 结构，防止模型附加说明文字污染题库数据。
            List<AiQuestionItem> items = parser.parse(raw).questions();
            if (items.isEmpty()) {
                throw BusinessException.badRequest("AI 未识别到题目");
            }
            return new AnalysisComputation(items, raw);
        } catch (BusinessException ex) {
            throw new AiAnalysisException(ex.getMessage(), raw, ex);
        }
    }

    /**
     * 汇总分片结果并同步更新解析批次和文档主状态。
     * @param document 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysis 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void finishAnalysis(ExamDocument document, ExamDocumentAnalysis analysis) {
        ChunkProgressResponse progress = chunkProgress(analysis.getId());
        if (progress.total() == 0 || progress.success() == 0 && progress.failed() > 0) {
            // 全部失败时文档仍可再次发起解析，但不会进入待确认题目列表。
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(progress.latestErrorMessage());
            document.setStatus(DocumentStatus.PARSE_FAILED);
        } else if (progress.failed() > 0) {
            // 部分失败时保留已成功导入的题目，同时提示用户存在未完成分片。
            analysis.setStatus(AnalysisStatus.PARTIAL_FAILED);
            analysis.setErrorMessage(progress.latestErrorMessage());
            document.setStatus(DocumentStatus.PARSE_PARTIAL_FAILED);
        } else {
            // 全部分片成功后，文档进入题目确认阶段，前端可展示解析出的题目。
            analysis.setStatus(AnalysisStatus.SUCCESS);
            analysis.setErrorMessage(null);
            document.setStatus(DocumentStatus.PENDING_CONFIRMATION);
        }
        analysisMapper.updateById(analysis);
        documentMapper.updateById(document);
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysisId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param items 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private List<QuestionImportResult> saveQuestions(Long documentId, Long analysisId, Long userId, List<AiQuestionItem> items)
            throws JsonProcessingException {
        return saveQuestions(documentId, analysisId, null, 1, userId, items);
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysisId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param chunkId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param startOrder 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param items 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private List<QuestionImportResult> saveQuestions(Long documentId, Long analysisId, Long chunkId, int startOrder,
                                                     Long userId, List<AiQuestionItem> items)
            throws JsonProcessingException {
        int order = startOrder;
        List<QuestionImportResult> results = new ArrayList<>();
        for (AiQuestionItem item : items) {
            results.add(questionBankService.importQuestion(item, documentId, analysisId, chunkId, order++, userId));
        }
        return results;
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private ExamDocument requireVisibleDocument(Long id) {
        ExamDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw BusinessException.badRequest("文档不存在");
        }
        if (!document.getCreateId().equals(CurrentUserUtils.currentUserId())) {
            throw BusinessException.forbidden();
        }
        return document;
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param createdBy 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private ExamDocumentAnalysis newAnalysis(Long documentId, Long createdBy) {
        ExamDocumentAnalysis analysis = new ExamDocumentAnalysis();
        analysis.setDocumentId(documentId);
        analysis.setModelName(modelName);
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setCreateId(createdBy);
        return analysis;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
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
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private ExamDocumentAnalysis latestAnalysisEntity(Long documentId) {
        return analysisMapper.selectOne(new LambdaQueryWrapper<ExamDocumentAnalysis>()
                .eq(ExamDocumentAnalysis::getDocumentId, documentId)
                .orderByDesc(ExamDocumentAnalysis::getId)
                .last("LIMIT 1"));
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param document 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param latestAnalysis 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private DocumentResponse toDocumentResponse(ExamDocument document, AnalysisSummary latestAnalysis) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getSha256(),
                document.getStatus(),
                document.getCreateId(),
                document.getCreateTime(),
                latestAnalysis
        );
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param analysis 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
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
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param analysis 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param importResults 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private AnalysisResponse toAnalysisResponse(ExamDocumentAnalysis analysis, List<QuestionImportResult> importResults) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getDocumentId(),
                analysis.getStatus(),
                analysis.getModelName(),
                analysis.getErrorMessage(),
                chunkProgress(analysis.getId()),
                analysis.getCreateTime(),
                importResults.stream().map(this::toQuestionResponse).toList()
        );
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param analysisId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private List<ExamDocumentAnalysisChunk> chunks(Long analysisId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysisId)
                .orderByAsc(ExamDocumentAnalysisChunk::getChunkIndex));
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param analysisId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private ChunkProgressResponse chunkProgress(Long analysisId) {
        List<ExamDocumentAnalysisChunk> chunks = chunks(analysisId);
        int success = 0;
        int failed = 0;
        int pending = 0;
        int processing = 0;
        int oversized = 0;
        String latestError = null;
        for (ExamDocumentAnalysisChunk chunk : chunks) {
            if (AnalysisChunkStatus.SUCCESS.equals(chunk.getStatus())) {
                success++;
            } else if (AnalysisChunkStatus.FAILED.equals(chunk.getStatus())) {
                failed++;
                latestError = chunk.getErrorMessage();
            } else if (AnalysisChunkStatus.PROCESSING.equals(chunk.getStatus())) {
                processing++;
            } else {
                pending++;
            }
            if (Boolean.TRUE.equals(chunk.getOversized())) {
                oversized++;
            }
        }
        return new ChunkProgressResponse(chunks.size(), success, failed, pending, processing, oversized, latestError);
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param chunk 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private int safeRetryCount(ExamDocumentAnalysisChunk chunk) {
        return chunk.getRetryCount() == null ? 0 : chunk.getRetryCount();
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param chunk 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private int chunkSortOrderBase(ExamDocumentAnalysisChunk chunk) {
        int chunkIndex = chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex();
        return chunkIndex * 10_000 + 1;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param existing 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param raw 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String appendRawJson(String existing, String raw) {
        if (raw == null || raw.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return raw;
        }
        return existing + "\n" + raw;
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param result 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private QuestionAnalysisResponse toQuestionResponse(QuestionImportResult result) {
        QuestionResponse question = questionBankService.toQuestionResponse(result.question());
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
                result.newlyCreated(),
                result.confidence(),
                result.sortOrder()
        );
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param source 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
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
     * AnalysisComputation 不可变业务数据记录，用于接口入参、接口返回或服务间传输。
     * @param items 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param rawJson 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private record AnalysisComputation(List<AiQuestionItem> items, String rawJson) {
    }

    /**
     * AI 分析异常，携带模型原始响应以便失败分片落库排查。
     */
    private static final class AiAnalysisException extends RuntimeException {
        private final String rawResponse;

        /**
         * 构造 AI 分析异常。
         * @param message 业务错误信息。
         * @param rawResponse 模型返回的原始文本。
         * @param cause 原始异常。
         */
        private AiAnalysisException(String message, String rawResponse, Throwable cause) {
            super(message, cause);
            this.rawResponse = rawResponse;
        }

        /**
         * 执行当前业务步骤，并返回调用方需要的处理结果。
         * @return 封装后的业务处理结果。
         */
        private String rawResponse() {
            return rawResponse;
        }
    }
}

