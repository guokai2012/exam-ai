package com.exam.ai.document.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.config.AiAnalysisProperties;
import com.exam.ai.document.entity.AnalysisChunkStatus;
import com.exam.ai.document.entity.AnalysisStatus;
import com.exam.ai.document.entity.DocumentStatus;
import com.exam.ai.document.entity.ExamDocument;
import com.exam.ai.document.entity.ExamDocumentAnalysis;
import com.exam.ai.document.entity.ExamDocumentAnalysisChunk;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.document.dto.AiQuestionResult;
import com.exam.ai.document.dto.AnalysisResponse;
import com.exam.ai.document.dto.AnalysisSummary;
import com.exam.ai.document.dto.ChunkProgressResponse;
import com.exam.ai.document.dto.DocumentContentResponse;
import com.exam.ai.document.dto.DocumentResponse;
import com.exam.ai.document.dto.QuestionAnalysisResponse;
import com.exam.ai.document.mapper.ExamDocumentAnalysisChunkMapper;
import com.exam.ai.document.mapper.ExamDocumentAnalysisMapper;
import com.exam.ai.document.mapper.ExamDocumentMapper;
import com.exam.ai.question.entity.ExamQuestionSource;
import com.exam.ai.question.dto.QuestionImportResult;
import com.exam.ai.question.dto.QuestionResponse;
import com.exam.ai.question.mapper.ExamQuestionSourceMapper;
import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.service.SystemConfigService;
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

@Service
public class DocumentService {

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

    public DocumentService(ExamDocumentMapper documentMapper, ExamDocumentAnalysisMapper analysisMapper,
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

    @Transactional(rollbackFor = Exception.class)
    public DocumentResponse upload(MultipartFile file, UserPrincipal principal) {
        DocumentFileService.StoredDocument stored = fileService.store(file);
        ExamDocument document = new ExamDocument();
        document.setOriginalFilename(stored.originalFilename());
        document.setStoredFilename(stored.storedFilename());
        document.setFileType(stored.fileType());
        document.setFileSize(stored.fileSize());
        document.setSha256(stored.sha256());
        document.setStoragePath(stored.storagePath());
        document.setExtractedText(stored.extractedText());
        document.setStatus(DocumentStatus.UPLOADED);
        document.setUploadedBy(principal.userId());
        documentMapper.insert(document);
        return toDocumentResponse(documentMapper.selectById(document.getId()), null);
    }

    public IPage<DocumentResponse> list(long page, long size, UserPrincipal principal) {
        LambdaQueryWrapper<ExamDocument> query = new LambdaQueryWrapper<ExamDocument>()
                .eq(ExamDocument::getUploadedBy, principal.userId())
                .orderByDesc(ExamDocument::getId);
        return documentMapper.selectPage(Page.of(page, size), query)
                .convert(document -> toDocumentResponse(document, latestSummary(document.getId())));
    }

    public DocumentResponse detail(Long id, UserPrincipal principal) {
        ExamDocument document = requireVisibleDocument(id, principal);
        return toDocumentResponse(document, latestSummary(id));
    }

    public DocumentContentResponse content(Long id, UserPrincipal principal) {
        ExamDocument document = requireVisibleDocument(id, principal);
        return new DocumentContentResponse(id, document.getExtractedText());
    }

    @Transactional(rollbackFor = Exception.class)
    public AnalysisResponse analyze(Long documentId, UserPrincipal principal) {
        ExamDocument document = requireVisibleDocument(documentId, principal);
        if (!List.of(DocumentStatus.UPLOADED, DocumentStatus.PARSE_FAILED, DocumentStatus.PARSE_PARTIAL_FAILED).contains(document.getStatus())) {
            throw BusinessException.badRequest("当前文档状态不允许 AI 解析");
        }
        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            throw BusinessException.badRequest("文档没有可分析的提取文本");
        }
        document.setStatus(DocumentStatus.PARSING);
        documentMapper.updateById(document);
        ExamDocumentAnalysis analysis = analysisForParsing(document, principal.userId());
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setErrorMessage(null);
        analysisMapper.updateById(analysis);
        processPendingChunks(document, analysis, principal.userId());
        finishAnalysis(document, analysis);
        return toAnalysisResponse(analysisMapper.selectById(analysis.getId()));
    }

    public AnalysisResponse latestAnalysis(Long documentId, UserPrincipal principal) {
        requireVisibleDocument(documentId, principal);
        ExamDocumentAnalysis analysis = latestAnalysisEntity(documentId);
        if (analysis == null) {
            throw BusinessException.badRequest("暂无分析结果");
        }
        return toAnalysisResponse(analysis);
    }

    private ExamDocumentAnalysis analysisForParsing(ExamDocument document, Long userId) {
        ExamDocumentAnalysis analysis = DocumentStatus.UPLOADED.equals(document.getStatus())
                ? null
                : latestAnalysisEntity(document.getId());
        if (analysis == null || chunks(analysis.getId()).isEmpty()) {
            analysis = newAnalysis(document.getId(), userId);
            analysis.setStatus(AnalysisStatus.PROCESSING);
            analysisMapper.insert(analysis);
            createChunks(document, analysis.getId());
        }
        return analysis;
    }

    private void createChunks(ExamDocument document, Long analysisId) {
        List<DocumentChunker.DocumentChunk> chunks = documentChunker.chunk(document.getExtractedText(), aiProperties.getMaxInputChars());
        for (DocumentChunker.DocumentChunk chunk : chunks) {
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

    private void processPendingChunks(ExamDocument document, ExamDocumentAnalysis analysis, Long userId) {
        List<ExamDocumentAnalysisChunk> pendingChunks = chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysis.getId())
                .in(ExamDocumentAnalysisChunk::getStatus, AnalysisChunkStatus.PENDING, AnalysisChunkStatus.FAILED, AnalysisChunkStatus.PROCESSING)
                .orderByAsc(ExamDocumentAnalysisChunk::getChunkIndex));
        for (ExamDocumentAnalysisChunk chunk : pendingChunks) {
            processChunk(document, analysis, chunk, userId);
        }
    }

    private void processChunk(ExamDocument document, ExamDocumentAnalysis analysis, ExamDocumentAnalysisChunk chunk, Long userId) {
        chunk.setStatus(AnalysisChunkStatus.PROCESSING);
        chunk.setStartedAt(LocalDateTime.now());
        chunk.setFinishedAt(null);
        chunk.setErrorMessage(null);
        chunkMapper.updateById(chunk);
        try {
            AnalysisComputation computation = analyzeChunkWithRetry(chunk.getChunkText());
            parser.validate(new AiQuestionResult(computation.items()));
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

    private AnalysisComputation analyzeChunkWithRetry(String text) throws JsonProcessingException {
        int attempts = 1 + Math.max(0, systemConfigService.aiDocumentAnalysisMaxRetries());
        Exception lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return analyzeChunk(text);
            } catch (Exception ex) {
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
            List<AiQuestionItem> items = parser.parse(raw).questions();
            if (items.isEmpty()) {
                throw BusinessException.badRequest("AI 未识别到题目");
            }
            return new AnalysisComputation(items, raw);
        } catch (BusinessException ex) {
            throw new AiAnalysisException(ex.getMessage(), raw, ex);
        }
    }

    private void finishAnalysis(ExamDocument document, ExamDocumentAnalysis analysis) {
        ChunkProgressResponse progress = chunkProgress(analysis.getId());
        if (progress.total() == 0 || progress.success() == 0 && progress.failed() > 0) {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setErrorMessage(progress.latestErrorMessage());
            document.setStatus(DocumentStatus.PARSE_FAILED);
        } else if (progress.failed() > 0) {
            analysis.setStatus(AnalysisStatus.PARTIAL_FAILED);
            analysis.setErrorMessage(progress.latestErrorMessage());
            document.setStatus(DocumentStatus.PARSE_PARTIAL_FAILED);
        } else {
            analysis.setStatus(AnalysisStatus.SUCCESS);
            analysis.setErrorMessage(null);
            document.setStatus(DocumentStatus.PENDING_CONFIRMATION);
        }
        analysisMapper.updateById(analysis);
        documentMapper.updateById(document);
    }

    private List<QuestionImportResult> saveQuestions(Long documentId, Long analysisId, Long userId, List<AiQuestionItem> items)
            throws JsonProcessingException {
        return saveQuestions(documentId, analysisId, null, 1, userId, items);
    }

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

    private ExamDocument requireVisibleDocument(Long id, UserPrincipal principal) {
        ExamDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw BusinessException.badRequest("文档不存在");
        }
        if (!document.getUploadedBy().equals(principal.userId())) {
            throw BusinessException.forbidden();
        }
        return document;
    }

    private ExamDocumentAnalysis newAnalysis(Long documentId, Long createdBy) {
        ExamDocumentAnalysis analysis = new ExamDocumentAnalysis();
        analysis.setDocumentId(documentId);
        analysis.setModelName(modelName);
        analysis.setStatus(AnalysisStatus.PROCESSING);
        analysis.setCreatedBy(createdBy);
        return analysis;
    }

    private AnalysisSummary latestSummary(Long documentId) {
        ExamDocumentAnalysis analysis = latestAnalysisEntity(documentId);
        if (analysis == null) {
            return null;
        }
        Long count = sourceMapper.selectCount(new LambdaQueryWrapper<ExamQuestionSource>()
                .eq(ExamQuestionSource::getAnalysisId, analysis.getId()));
        return new AnalysisSummary(analysis.getId(), analysis.getStatus(), analysis.getModelName(),
                count == null ? 0 : count.intValue(), analysis.getErrorMessage(), chunkProgress(analysis.getId()), analysis.getCreatedAt());
    }

    private ExamDocumentAnalysis latestAnalysisEntity(Long documentId) {
        return analysisMapper.selectOne(new LambdaQueryWrapper<ExamDocumentAnalysis>()
                .eq(ExamDocumentAnalysis::getDocumentId, documentId)
                .orderByDesc(ExamDocumentAnalysis::getId)
                .last("LIMIT 1"));
    }

    private DocumentResponse toDocumentResponse(ExamDocument document, AnalysisSummary latestAnalysis) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getFileType(),
                document.getFileSize(),
                document.getSha256(),
                document.getStatus(),
                document.getUploadedBy(),
                document.getCreatedAt(),
                latestAnalysis
        );
    }

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
                analysis.getCreatedAt(),
                sources.stream().map(this::toQuestionResponse).toList()
        );
    }

    private AnalysisResponse toAnalysisResponse(ExamDocumentAnalysis analysis, List<QuestionImportResult> importResults) {
        return new AnalysisResponse(
                analysis.getId(),
                analysis.getDocumentId(),
                analysis.getStatus(),
                analysis.getModelName(),
                analysis.getErrorMessage(),
                chunkProgress(analysis.getId()),
                analysis.getCreatedAt(),
                importResults.stream().map(this::toQuestionResponse).toList()
        );
    }

    private List<ExamDocumentAnalysisChunk> chunks(Long analysisId) {
        return chunkMapper.selectList(new LambdaQueryWrapper<ExamDocumentAnalysisChunk>()
                .eq(ExamDocumentAnalysisChunk::getAnalysisId, analysisId)
                .orderByAsc(ExamDocumentAnalysisChunk::getChunkIndex));
    }

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

    private int safeRetryCount(ExamDocumentAnalysisChunk chunk) {
        return chunk.getRetryCount() == null ? 0 : chunk.getRetryCount();
    }

    private int chunkSortOrderBase(ExamDocumentAnalysisChunk chunk) {
        int chunkIndex = chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex();
        return chunkIndex * 10_000 + 1;
    }

    private String appendRawJson(String existing, String raw) {
        if (raw == null || raw.isBlank()) {
            return existing;
        }
        if (existing == null || existing.isBlank()) {
            return raw;
        }
        return existing + "\n" + raw;
    }

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

    private record AnalysisComputation(List<AiQuestionItem> items, String rawJson) {
    }

    private static final class AiAnalysisException extends RuntimeException {
        private final String rawResponse;

        private AiAnalysisException(String message, String rawResponse, Throwable cause) {
            super(message, cause);
            this.rawResponse = rawResponse;
        }

        private String rawResponse() {
            return rawResponse;
        }
    }
}

