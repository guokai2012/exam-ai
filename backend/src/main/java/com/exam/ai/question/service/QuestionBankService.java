package com.exam.ai.question.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.question.entity.ExamQuestionBank;
import com.exam.ai.question.entity.ExamQuestionCategory;
import com.exam.ai.question.entity.ExamQuestionSource;
import com.exam.ai.question.entity.ExamQuestionTag;
import com.exam.ai.question.entity.ExamQuestionTagRelation;
import com.exam.ai.question.entity.QuestionCategoryStatus;
import com.exam.ai.question.entity.QuestionEvent;
import com.exam.ai.question.entity.QuestionState;
import com.exam.ai.question.dto.CreateQuestionCategoryRequest;
import com.exam.ai.question.dto.QuestionCategoryResponse;
import com.exam.ai.question.dto.QuestionImportResult;
import com.exam.ai.question.dto.QuestionResponse;
import com.exam.ai.question.dto.ReviewQuestionRequest;
import com.exam.ai.question.mapper.ExamQuestionBankMapper;
import com.exam.ai.question.mapper.ExamQuestionCategoryMapper;
import com.exam.ai.question.mapper.ExamQuestionSourceMapper;
import com.exam.ai.question.mapper.ExamQuestionTagMapper;
import com.exam.ai.question.mapper.ExamQuestionTagRelationMapper;
import com.exam.ai.security.UserPrincipal;
import com.exam.ai.system.service.NotificationService;
import com.exam.ai.user.mapper.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuestionBankService {

    private static final String DEFAULT_CATEGORY = "默认题库";

    private final ExamQuestionCategoryMapper categoryMapper;
    private final ExamQuestionBankMapper questionMapper;
    private final ExamQuestionSourceMapper sourceMapper;
    private final ExamQuestionTagMapper tagMapper;
    private final ExamQuestionTagRelationMapper tagRelationMapper;
    private final QuestionStemNormalizer stemNormalizer;
    private final QuestionStateTransitionService stateTransitionService;
    private final NotificationService notificationService;
    private final SysUserMapper userMapper;
    private final ObjectMapper objectMapper;

    public QuestionBankService(ExamQuestionCategoryMapper categoryMapper, ExamQuestionBankMapper questionMapper,
                               ExamQuestionSourceMapper sourceMapper, ExamQuestionTagMapper tagMapper,
                               ExamQuestionTagRelationMapper tagRelationMapper, QuestionStemNormalizer stemNormalizer,
                               QuestionStateTransitionService stateTransitionService,
                               NotificationService notificationService, SysUserMapper userMapper,
                               ObjectMapper objectMapper) {
        this.categoryMapper = categoryMapper;
        this.questionMapper = questionMapper;
        this.sourceMapper = sourceMapper;
        this.tagMapper = tagMapper;
        this.tagRelationMapper = tagRelationMapper;
        this.stemNormalizer = stemNormalizer;
        this.stateTransitionService = stateTransitionService;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionImportResult importQuestion(AiQuestionItem item, Long documentId, Long analysisId, int sortOrder,
                                               Long userId) throws JsonProcessingException {
        return importQuestion(item, documentId, analysisId, null, sortOrder, userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionImportResult importQuestion(AiQuestionItem item, Long documentId, Long analysisId, Long chunkId, int sortOrder,
                                               Long userId) throws JsonProcessingException {
        ExamQuestionCategory category = findOrCreateCategory(item.categoryName(), userId);
        String normalizedStem = stemNormalizer.normalize(item.stem());
        String stemHash = stemNormalizer.hash(normalizedStem);
        ExamQuestionBank question = questionMapper.selectOne(new LambdaQueryWrapper<ExamQuestionBank>()
                .eq(ExamQuestionBank::getCreatedBy, userId)
                .eq(ExamQuestionBank::getCategoryId, category.getId())
                .eq(ExamQuestionBank::getStemHash, stemHash)
                .last("LIMIT 1"));
        boolean newlyCreated = false;
        if (question == null) {
            question = new ExamQuestionBank();
            question.setCategoryId(category.getId());
            question.setQuestionType(item.type());
            question.setStem(item.stem());
            question.setNormalizedStem(normalizedStem);
            question.setStemHash(stemHash);
            question.setOptionsJson(objectMapper.writeValueAsString(item.options() == null ? List.of() : item.options()));
            question.setStandardAnswer(item.standardAnswer());
            question.setExplanation(item.explanation());
            question.setDifficultyStars(item.difficultyStars());
            question.setState(QuestionState.PARSE_PENDING_CONFIRM.name());
            question.setTagRetryCount(0);
            question.setCreatedBy(userId);
            questionMapper.insert(question);
            newlyCreated = true;
        }
        ExamQuestionSource source = new ExamQuestionSource();
        source.setQuestionId(question.getId());
        source.setDocumentId(documentId);
        source.setAnalysisId(analysisId);
        source.setChunkId(chunkId);
        source.setConfidence(item.confidence());
        source.setSortOrder(sortOrder);
        sourceMapper.insert(source);
        return new QuestionImportResult(question, category, item, item.confidence(), sortOrder, newlyCreated);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamQuestionCategory findOrCreateCategory(String categoryName, Long userId) {
        String normalized = normalizeCategoryName(categoryName);
        ExamQuestionCategory existing = categoryMapper.selectOne(new LambdaQueryWrapper<ExamQuestionCategory>()
                .eq(ExamQuestionCategory::getCategoryName, normalized)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        ExamQuestionCategory category = new ExamQuestionCategory();
        category.setCategoryName(normalized);
        category.setDescription("AI 自动分类");
        category.setStatus(QuestionCategoryStatus.ENABLED);
        category.setCreatedBy(userId);
        categoryMapper.insert(category);
        return category;
    }

    public List<QuestionCategoryResponse> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ExamQuestionCategory>()
                        .eq(ExamQuestionCategory::getStatus, QuestionCategoryStatus.ENABLED)
                        .orderByAsc(ExamQuestionCategory::getId))
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionCategoryResponse createCategory(CreateQuestionCategoryRequest request, UserPrincipal principal) {
        ExamQuestionCategory category = findOrCreateCategory(request.categoryName(), principal.userId());
        if (request.description() != null && !request.description().isBlank()) {
            category.setDescription(request.description());
            categoryMapper.updateById(category);
        }
        return toCategoryResponse(categoryMapper.selectById(category.getId()));
    }

    public IPage<QuestionResponse> listQuestions(long page, long size, Long categoryId, String questionType,
                                                 String state, Long tagId, UserPrincipal principal) {
        LambdaQueryWrapper<ExamQuestionBank> query = new LambdaQueryWrapper<ExamQuestionBank>()
                .eq(ExamQuestionBank::getCreatedBy, principal.userId())
                .orderByDesc(ExamQuestionBank::getId);
        if (categoryId != null) {
            query.eq(ExamQuestionBank::getCategoryId, categoryId);
        }
        if (questionType != null && !questionType.isBlank()) {
            query.eq(ExamQuestionBank::getQuestionType, questionType);
        }
        if (state != null && !state.isBlank()) {
            query.eq(ExamQuestionBank::getState, state);
        }
        if (tagId != null) {
            query.inSql(ExamQuestionBank::getId, "SELECT question_id FROM exam_question_tag_relation WHERE tag_id = " + tagId);
        }
        return questionMapper.selectPage(Page.of(page, size), query).convert(this::toQuestionResponse);
    }

    public QuestionResponse detail(Long id) {
        return toQuestionResponse(questionMapper.selectById(id));
    }

    public QuestionResponse detail(Long id, UserPrincipal principal) {
        ExamQuestionBank question = requireQuestion(id);
        requireOwner(question, principal);
        return toQuestionResponse(question);
    }

    @Transactional(rollbackFor = Exception.class)
    public QuestionResponse review(Long id, ReviewQuestionRequest request, UserPrincipal principal) {
        ExamQuestionBank question = requireQuestion(id);
        requireOwner(question, principal);
        if (request.categoryId() != null && !request.categoryId().equals(question.getCategoryId())) {
            moveCategoryWithDuplicateCheck(question, request.categoryId());
        }
        QuestionEvent event = Boolean.TRUE.equals(request.approved()) ? QuestionEvent.CONFIRM_PARSED : QuestionEvent.REJECT_PARSED;
        QuestionState next = stateTransitionService.transit(id, question.getState(), event);
        question.setState(next.name());
        question.setReviewedBy(principal.userId());
        question.setReviewedAt(LocalDateTime.now());
        question.setReviewReason(request.reason());
        question.setTagErrorMessage(null);
        question.setTagRetryCount(0);
        question.setTagNotifiedAt(null);
        questionMapper.updateById(question);
        return toQuestionResponse(questionMapper.selectById(id));
    }

    public List<ExamQuestionBank> tagCandidates(int limit, int maxRetries) {
        LambdaQueryWrapper<ExamQuestionBank> query = new LambdaQueryWrapper<ExamQuestionBank>()
                .orderByAsc(ExamQuestionBank::getUpdatedAt)
                .last("LIMIT " + limit);
        if (maxRetries <= 0) {
            query.eq(ExamQuestionBank::getState, QuestionState.TAG_PENDING.name());
        } else {
            query.and(wrapper -> wrapper
                    .eq(ExamQuestionBank::getState, QuestionState.TAG_PENDING.name())
                    .or(failed -> failed
                            .eq(ExamQuestionBank::getState, QuestionState.TAG_FAILED.name())
                            .isNull(ExamQuestionBank::getTagNotifiedAt)
                            .and(retry -> retry
                                    .isNull(ExamQuestionBank::getTagRetryCount)
                                    .or()
                                    .lt(ExamQuestionBank::getTagRetryCount, maxRetries))));
        }
        return questionMapper.selectList(query);
    }

    @Transactional(rollbackFor = Exception.class)
    public ExamQuestionBank startTagging(ExamQuestionBank question) {
        boolean retry = QuestionState.TAG_FAILED.name().equals(question.getState());
        QuestionEvent event = retry ? QuestionEvent.RETRY_TAGGING : QuestionEvent.START_TAGGING;
        QuestionState next = stateTransitionService.transit(question.getId(), question.getState(), event);
        question.setState(next.name());
        question.setTagErrorMessage(null);
        if (retry) {
            question.setTagRetryCount(safeRetryCount(question) + 1);
        }
        questionMapper.updateById(question);
        return questionMapper.selectById(question.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void tagSuccess(Long questionId, List<String> tagNames) {
        ExamQuestionBank question = requireQuestion(questionId);
        tagRelationMapper.delete(new LambdaUpdateWrapper<ExamQuestionTagRelation>()
                .eq(ExamQuestionTagRelation::getQuestionId, questionId));
        for (String tagName : tagNames) {
            // 标签需要按名称即时查找或创建，关系写入依赖每个标签的最终主键。
            ExamQuestionTag tag = findOrCreateTag(tagName);
            tagRelationMapper.insert(new ExamQuestionTagRelation(questionId, tag.getId(), LocalDateTime.now()));
        }
        QuestionState next = stateTransitionService.transit(questionId, question.getState(), QuestionEvent.TAG_SUCCESS);
        question.setState(next.name());
        question.setTagErrorMessage(null);
        questionMapper.updateById(question);
    }

    @Transactional(rollbackFor = Exception.class)
    public void tagFailed(Long questionId, String errorMessage, int maxRetries) {
        ExamQuestionBank question = requireQuestion(questionId);
        QuestionState next = stateTransitionService.transit(questionId, question.getState(), QuestionEvent.TAG_FAIL);
        question.setState(next.name());
        question.setTagErrorMessage(errorMessage);
        if (safeRetryCount(question) >= maxRetries && question.getTagNotifiedAt() == null) {
            notifyTaggingFailed(question, maxRetries);
            question.setTagNotifiedAt(LocalDateTime.now());
        }
        questionMapper.updateById(question);
    }

    public String normalizeCategoryName(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            normalized = DEFAULT_CATEGORY;
        }
        return normalized.length() > 128 ? normalized.substring(0, 128) : normalized;
    }

    public QuestionResponse toQuestionResponse(ExamQuestionBank question) {
        if (question == null) {
            return null;
        }
        ExamQuestionCategory category = categoryMapper.selectById(question.getCategoryId());
        List<String> options;
        try {
            options = objectMapper.readValue(question.getOptionsJson(), objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception ex) {
            options = List.of();
        }
        return new QuestionResponse(
                question.getId(),
                question.getCategoryId(),
                category == null ? "" : category.getCategoryName(),
                question.getQuestionType(),
                question.getStem(),
                options,
                question.getStandardAnswer(),
                question.getExplanation(),
                question.getDifficultyStars(),
                question.getState(),
                question.getReviewReason(),
                question.getTagErrorMessage(),
                safeRetryCount(question),
                question.getTagNotifiedAt() != null,
                tags(question.getId()),
                question.getCreatedAt()
        );
    }

    private QuestionCategoryResponse toCategoryResponse(ExamQuestionCategory category) {
        return new QuestionCategoryResponse(category.getId(), category.getCategoryName(), category.getDescription(), category.getStatus());
    }

    private ExamQuestionBank requireQuestion(Long id) {
        ExamQuestionBank question = questionMapper.selectById(id);
        if (question == null) {
            throw BusinessException.badRequest("题目不存在");
        }
        return question;
    }

    private void requireOwner(ExamQuestionBank question, UserPrincipal principal) {
        if (!principal.userId().equals(question.getCreatedBy())) {
            throw BusinessException.forbidden();
        }
    }

    private void moveCategoryWithDuplicateCheck(ExamQuestionBank question, Long categoryId) {
        ExamQuestionCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !Integer.valueOf(QuestionCategoryStatus.ENABLED).equals(category.getStatus())) {
            throw BusinessException.badRequest("题库分类不存在或已停用");
        }
        ExamQuestionBank duplicate = questionMapper.selectOne(new LambdaQueryWrapper<ExamQuestionBank>()
                .eq(ExamQuestionBank::getCreatedBy, question.getCreatedBy())
                .eq(ExamQuestionBank::getCategoryId, categoryId)
                .eq(ExamQuestionBank::getStemHash, question.getStemHash())
                .ne(ExamQuestionBank::getId, question.getId())
                .last("LIMIT 1"));
        if (duplicate != null) {
            throw BusinessException.conflict("目标分类下已存在相同题干题目");
        }
        question.setCategoryId(categoryId);
    }

    private ExamQuestionTag findOrCreateTag(String tagName) {
        String normalized = normalizeTagName(tagName);
        ExamQuestionTag existing = tagMapper.selectOne(new LambdaQueryWrapper<ExamQuestionTag>()
                .eq(ExamQuestionTag::getTagName, normalized)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        ExamQuestionTag tag = new ExamQuestionTag();
        tag.setTagName(normalized);
        tag.setDescription("AI 自动标签");
        tag.setStatus(QuestionCategoryStatus.ENABLED);
        tagMapper.insert(tag);
        return tag;
    }

    private String normalizeTagName(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            normalized = "未分类标签";
        }
        return normalized.length() > 128 ? normalized.substring(0, 128) : normalized;
    }

    private List<String> tags(Long questionId) {
        List<ExamQuestionTagRelation> relations = tagRelationMapper.selectList(new LambdaQueryWrapper<ExamQuestionTagRelation>()
                .eq(ExamQuestionTagRelation::getQuestionId, questionId));
        if (relations.isEmpty()) {
            return List.of();
        }
        return tagMapper.selectBatchIds(relations.stream().map(ExamQuestionTagRelation::getTagId).toList())
                .stream()
                .map(ExamQuestionTag::getTagName)
                .toList();
    }

    private int safeRetryCount(ExamQuestionBank question) {
        return question.getTagRetryCount() == null ? 0 : question.getTagRetryCount();
    }

    private void notifyTaggingFailed(ExamQuestionBank question, int maxRetries) {
        if (question.getCreatedBy() == null || userMapper.selectById(question.getCreatedBy()) == null) {
            return;
        }
        String title = "题目 AI 标签分析失败";
        String content = "题目 #" + question.getId() + " 在首次分析失败后已重试 "
                + maxRetries + " 次，仍未生成题型标签，请人工处理。题干："
                + abbreviate(question.getStem(), 120);
        notificationService.create(
                question.getCreatedBy(),
                title,
                content,
                NotificationService.TYPE_AI_TAGGING_FAILED,
                NotificationService.BUSINESS_QUESTION,
                question.getId()
        );
    }

    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}

