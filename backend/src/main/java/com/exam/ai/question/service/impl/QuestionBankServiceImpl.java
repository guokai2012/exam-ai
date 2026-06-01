package com.exam.ai.question.service.impl;

import com.exam.ai.question.service.QuestionBankService;
import com.exam.ai.question.service.QuestionStateTransitionService;
import com.exam.ai.question.util.QuestionStemNormalizer;
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
import com.exam.ai.question.vo.QuestionCategoryResponse;
import com.exam.ai.question.dto.QuestionImportResult;
import com.exam.ai.question.vo.QuestionResponse;
import com.exam.ai.question.dto.ReviewQuestionRequest;
import com.exam.ai.question.mapper.ExamQuestionBankMapper;
import com.exam.ai.question.mapper.ExamQuestionCategoryMapper;
import com.exam.ai.question.mapper.ExamQuestionSourceMapper;
import com.exam.ai.question.mapper.ExamQuestionTagMapper;
import com.exam.ai.question.mapper.ExamQuestionTagRelationMapper;
import com.exam.ai.system.service.NotificationService;
import com.exam.ai.util.CurrentUserUtils;
import com.exam.ai.user.mapper.SysUserMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 题库业务实现类，负责 AI 题目入库、人工确认、分类维护、自动标签和前端题目视图转换。
 *
 * <p>题目入库以“创建人 + 分类 + 标准化题干哈希”作为去重条件，避免同一用户多次解析相同文档时
 * 产生重复题目，同时保留每次解析来源记录用于追溯。</p>
 */
@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    private static final String DEFAULT_CATEGORY = "默认题库";
    private static final String DEFAULT_TAG = "未分类标签";
    private static final String AI_AUTO_CATEGORY_DESCRIPTION = "AI 自动分类";
    private static final String AI_AUTO_TAG_DESCRIPTION = "AI 自动标签";
    private static final int MAX_CATEGORY_NAME_LENGTH = 128;
    private static final int MAX_TAG_NAME_LENGTH = 128;
    private static final int TAG_FAILURE_STEM_PREVIEW_LENGTH = 120;

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

    /**
     * 构造题库业务服务。
     *
     * @param categoryMapper 题目分类表访问器。
     * @param questionMapper 题库题目表访问器。
     * @param sourceMapper 题目来源表访问器，用于记录文档解析来源。
     * @param tagMapper 标签表访问器。
     * @param tagRelationMapper 题目标签关系表访问器。
     * @param stemNormalizer 题干标准化与哈希工具。
     * @param stateTransitionService 题目状态机服务。
     * @param notificationService 站内通知服务，用于标签失败后提醒题目创建者。
     * @param userMapper 用户表访问器，用于通知前确认创建者仍存在。
     * @param objectMapper JSON 序列化工具，用于读写题目选项。
     */
    public QuestionBankServiceImpl(ExamQuestionCategoryMapper categoryMapper, ExamQuestionBankMapper questionMapper,
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

    /**
     * 导入 AI 识别出的题目，并绑定文档、解析批次和排序信息。
     *
     * @param item AI 返回的单题结构。
     * @param documentId 来源文档 ID。
     * @param analysisId 文档分析批次 ID。
     * @param sortOrder 题目在分析结果中的顺序。
     * @param userId 题目归属用户 ID。
     * @return 题目入库结果，包含是否新建和来源排序。
     * @throws JsonProcessingException 当题目选项无法序列化为 JSON 时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionImportResult importQuestion(AiQuestionItem item, Long documentId, Long analysisId, int sortOrder,
                                               Long userId) throws JsonProcessingException {
        return importQuestion(item, documentId, analysisId, null, sortOrder, userId);
    }

    /**
     * 导入 AI 识别出的题目，并记录可选分片来源。
     *
     * @param item AI 返回的单题结构。
     * @param documentId 来源文档 ID。
     * @param analysisId 文档分析批次 ID。
     * @param chunkId 文档分片 ID，整篇分析场景可为空。
     * @param sortOrder 题目在分析结果中的顺序。
     * @param userId 题目归属用户 ID。
     * @return 题目入库结果，包含分类、题目、来源和是否新建。
     * @throws JsonProcessingException 当题目选项无法序列化为 JSON 时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionImportResult importQuestion(AiQuestionItem item, Long documentId, Long analysisId, Long chunkId, int sortOrder,
                                               Long userId) throws JsonProcessingException {
        ExamQuestionCategory category = findOrCreateCategory(item.categoryName(), userId);
        String normalizedStem = stemNormalizer.normalize(item.stem());
        String stemHash = stemNormalizer.hash(normalizedStem);
        // 同一用户同一分类下按题干哈希去重，避免重复解析或重复上传造成题库膨胀。
        ExamQuestionBank question = questionMapper.selectOne(new LambdaQueryWrapper<ExamQuestionBank>()
                .eq(ExamQuestionBank::getCreatedBy, userId)
                .eq(ExamQuestionBank::getCategoryId, category.getId())
                .eq(ExamQuestionBank::getStemHash, stemHash)
                .last("LIMIT 1"));
        boolean newlyCreated = false;
        if (question == null) {
            // 新题先进入待确认状态，必须由用户确认后才进入可用题库。
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
        // 来源记录每次解析都要新增，用于追溯题目来自哪个文档、批次和分片。
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

    /**
     * 查询或创建题目分类，AI 未给出分类时归入默认题库。
     * @param categoryName 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamQuestionCategory findOrCreateCategory(String categoryName, Long userId) {
        String normalized = normalizeCategoryName(categoryName);
        ExamQuestionCategory existing = categoryMapper.selectOne(new LambdaQueryWrapper<ExamQuestionCategory>()
                .eq(ExamQuestionCategory::getCategoryName, normalized)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        // 分类由 AI 自动创建时只保存启用状态，后续可由管理端维护描述。
        ExamQuestionCategory category = new ExamQuestionCategory();
        category.setCategoryName(normalized);
        category.setDescription(AI_AUTO_CATEGORY_DESCRIPTION);
        category.setStatus(QuestionCategoryStatus.ENABLED);
        category.setCreatedBy(userId);
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<QuestionCategoryResponse> categories() {
        return categoryMapper.selectList(new LambdaQueryWrapper<ExamQuestionCategory>()
                        .eq(ExamQuestionCategory::getStatus, QuestionCategoryStatus.ENABLED)
                        .orderByAsc(ExamQuestionCategory::getId))
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionCategoryResponse createCategory(CreateQuestionCategoryRequest request) {
        ExamQuestionCategory category = findOrCreateCategory(request.categoryName(), CurrentUserUtils.currentUserId());
        if (request.description() != null && !request.description().isBlank()) {
            category.setDescription(request.description());
            categoryMapper.updateById(category);
        }
        return toCategoryResponse(categoryMapper.selectById(category.getId()));
    }

    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param categoryId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param questionType 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param state 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param tagId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<QuestionResponse> listQuestions(long page, long size, Long categoryId, String questionType,
                                                 String state, Long tagId) {
        LambdaQueryWrapper<ExamQuestionBank> query = new LambdaQueryWrapper<ExamQuestionBank>()
                .eq(ExamQuestionBank::getCreatedBy, CurrentUserUtils.currentUserId())
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

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionResponse detail(Long id) {
        return toQuestionResponse(questionMapper.selectById(id));
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionResponse detailForCurrentUser(Long id) {
        ExamQuestionBank question = requireQuestion(id);
        requireOwner(question);
        return toQuestionResponse(question);
    }

    /**
     * 审核待确认题目并执行确认或驳回状态流转。
     *
     * @param id 题目 ID。
     * @param request 审核请求，包含是否通过、目标分类和审核原因。
     * @return 审核后的题目视图对象。
     * @throws BusinessException 当题目不存在、当前用户不是创建者、目标分类无效或状态不允许审核时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public QuestionResponse review(Long id, ReviewQuestionRequest request) {
        ExamQuestionBank question = requireQuestion(id);
        requireOwner(question);
        if (request.categoryId() != null && !request.categoryId().equals(question.getCategoryId())) {
            // 移动分类时必须重新做目标分类下的题干去重检查。
            moveCategoryWithDuplicateCheck(question, request.categoryId());
        }
        QuestionEvent event = Boolean.TRUE.equals(request.approved()) ? QuestionEvent.CONFIRM_PARSED : QuestionEvent.REJECT_PARSED;
        // 状态变化统一通过状态机校验，避免 Controller 或前端绕过允许的业务流转。
        QuestionState next = stateTransitionService.transit(id, question.getState(), event);
        question.setState(next.name());
        question.setReviewedBy(CurrentUserUtils.currentUserId());
        question.setReviewedAt(LocalDateTime.now());
        question.setReviewReason(request.reason());
        question.setTagErrorMessage(null);
        question.setTagRetryCount(0);
        question.setTagNotifiedAt(null);
        questionMapper.updateById(question);
        return toQuestionResponse(questionMapper.selectById(id));
    }

    /**
     * 查询待执行 AI 标签任务的题目。
     *
     * @param limit 单次调度最多处理的题目数量。
     * @param maxRetries 标签失败题目的最大重试次数，小于等于 0 时只处理首次待标签题目。
     * @return 按更新时间升序排列的候选题目。
     */
    public List<ExamQuestionBank> tagCandidates(int limit, int maxRetries) {
        LambdaQueryWrapper<ExamQuestionBank> query = new LambdaQueryWrapper<ExamQuestionBank>()
                .orderByAsc(ExamQuestionBank::getUpdatedAt)
                .last("LIMIT " + limit);
        if (maxRetries <= 0) {
            // 不允许重试时只挑选首次待标签题，避免失败题目反复进入调度。
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

    /**
     * 标记题目开始执行自动标签任务。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public ExamQuestionBank startTagging(ExamQuestionBank question) {
        boolean retry = QuestionState.TAG_FAILED.name().equals(question.getState());
        QuestionEvent event = retry ? QuestionEvent.RETRY_TAGGING : QuestionEvent.START_TAGGING;
        // 标签任务也走状态机，确保重试只发生在允许重试的失败状态。
        QuestionState next = stateTransitionService.transit(question.getId(), question.getState(), event);
        question.setState(next.name());
        question.setTagErrorMessage(null);
        if (retry) {
            question.setTagRetryCount(safeRetryCount(question) + 1);
        }
        questionMapper.updateById(question);
        return questionMapper.selectById(question.getId());
    }

    /**
     * 保存 AI 自动标签并将题目标记为标签处理完成。
     * @param questionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param tagNames 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void tagSuccess(Long questionId, List<String> tagNames) {
        ExamQuestionBank question = requireQuestion(questionId);
        tagRelationMapper.delete(new LambdaUpdateWrapper<ExamQuestionTagRelation>()
                .eq(ExamQuestionTagRelation::getQuestionId, questionId));
        // 先清理旧关系再写入新标签，保证同一题目的标签集合与本次 AI 输出一致。
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

    /**
     * 记录自动标签失败原因，达到最大重试次数后给题目创建者发送通知。
     * @param questionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param errorMessage 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param maxRetries 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Transactional(rollbackFor = Exception.class)
    public void tagFailed(Long questionId, String errorMessage, int maxRetries) {
        ExamQuestionBank question = requireQuestion(questionId);
        QuestionState next = stateTransitionService.transit(questionId, question.getState(), QuestionEvent.TAG_FAIL);
        question.setState(next.name());
        question.setTagErrorMessage(errorMessage);
        if (safeRetryCount(question) >= maxRetries && question.getTagNotifiedAt() == null) {
            // 通知只发送一次，避免定时任务反复失败时刷屏。
            notifyTaggingFailed(question, maxRetries);
            question.setTagNotifiedAt(LocalDateTime.now());
        }
        questionMapper.updateById(question);
    }

    /**
     * 标准化分类名称，空分类统一归入默认题库并限制数据库字段长度。
     *
     * @param value 原始分类名称。
     * @return 标准化后的分类名称。
     */
    public String normalizeCategoryName(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            normalized = DEFAULT_CATEGORY;
        }
        return normalized.length() > MAX_CATEGORY_NAME_LENGTH ? normalized.substring(0, MAX_CATEGORY_NAME_LENGTH) : normalized;
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param category 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private QuestionCategoryResponse toCategoryResponse(ExamQuestionCategory category) {
        return new QuestionCategoryResponse(category.getId(), category.getCategoryName(), category.getDescription(), category.getStatus());
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private ExamQuestionBank requireQuestion(Long id) {
        ExamQuestionBank question = questionMapper.selectById(id);
        if (question == null) {
            throw BusinessException.badRequest("题目不存在");
        }
        return question;
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param principal 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void requireOwner(ExamQuestionBank question) {
        if (!CurrentUserUtils.currentUserId().equals(question.getCreatedBy())) {
            throw BusinessException.forbidden();
        }
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param categoryId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
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

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param tagName 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
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
        tag.setDescription(AI_AUTO_TAG_DESCRIPTION);
        tag.setStatus(QuestionCategoryStatus.ENABLED);
        tagMapper.insert(tag);
        return tag;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param value 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String normalizeTagName(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            normalized = DEFAULT_TAG;
        }
        return normalized.length() > MAX_TAG_NAME_LENGTH ? normalized.substring(0, MAX_TAG_NAME_LENGTH) : normalized;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param questionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
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

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private int safeRetryCount(ExamQuestionBank question) {
        return question.getTagRetryCount() == null ? 0 : question.getTagRetryCount();
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param maxRetries 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     */
    private void notifyTaggingFailed(ExamQuestionBank question, int maxRetries) {
        if (question.getCreatedBy() == null || userMapper.selectById(question.getCreatedBy()) == null) {
            return;
        }
        String title = "题目 AI 标签分析失败";
        String content = "题目 #" + question.getId() + " 在首次分析失败后已重试 "
                + maxRetries + " 次，仍未生成题型标签，请人工处理。题干："
                + abbreviate(question.getStem(), TAG_FAILURE_STEM_PREVIEW_LENGTH);
        notificationService.create(
                question.getCreatedBy(),
                title,
                content,
                NotificationService.TYPE_AI_TAGGING_FAILED,
                NotificationService.BUSINESS_QUESTION,
                question.getId()
        );
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param value 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param maxLength 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String abbreviate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}

