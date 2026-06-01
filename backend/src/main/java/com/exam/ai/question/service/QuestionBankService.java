package com.exam.ai.question.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.question.entity.ExamQuestionBank;
import com.exam.ai.question.entity.ExamQuestionCategory;
import com.exam.ai.question.dto.CreateQuestionCategoryRequest;
import com.exam.ai.question.vo.QuestionCategoryResponse;
import com.exam.ai.question.dto.QuestionImportResult;
import com.exam.ai.question.vo.QuestionResponse;
import com.exam.ai.question.dto.ReviewQuestionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.List;

/**
 * QuestionBankService 接口，定义当前业务模块对外提供的服务契约。
 */
public interface QuestionBankService {

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param item 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysisId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param sortOrder 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionImportResult importQuestion(AiQuestionItem item, Long documentId, Long analysisId, int sortOrder, Long userId) throws JsonProcessingException;
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param item 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param documentId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param analysisId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param chunkId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param sortOrder 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionImportResult importQuestion(AiQuestionItem item, Long documentId, Long analysisId, Long chunkId, int sortOrder, Long userId) throws JsonProcessingException;
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param categoryName 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param userId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public ExamQuestionCategory findOrCreateCategory(String categoryName, Long userId);
    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<QuestionCategoryResponse> categories();
    /**
     * 创建业务数据并完成必要的默认状态初始化。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionCategoryResponse createCategory(CreateQuestionCategoryRequest request);
    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param page 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param size 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param categoryId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param questionType 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param state 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param tagId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public IPage<QuestionResponse> listQuestions(long page, long size, Long categoryId, String questionType, String state, Long tagId);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionResponse detail(Long id);
    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionResponse detailForCurrentUser(Long id);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param id 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param request 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionResponse review(Long id, ReviewQuestionRequest request);
    /**
     * 查询业务数据集合，并按调用场景组织返回结构。
     * @param limit 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param maxRetries 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<ExamQuestionBank> tagCandidates(int limit, int maxRetries);
    /**
     * 更新业务状态，并保持相关数据的一致性。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public ExamQuestionBank startTagging(ExamQuestionBank question);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param questionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param tagNames 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void tagSuccess(Long questionId, List<String> tagNames);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param questionId 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param errorMessage 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @param maxRetries 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void tagFailed(Long questionId, String errorMessage, int maxRetries);
    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param value 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String normalizeCategoryName(String value);
    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param question 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionResponse toQuestionResponse(ExamQuestionBank question);
}
