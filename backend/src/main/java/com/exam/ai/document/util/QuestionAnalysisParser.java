package com.exam.ai.document.util;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiAssembledQuestionItem;
import com.exam.ai.document.dto.AiDocumentAssembleResult;
import com.exam.ai.document.dto.AiPageAnalysisResult;
import com.exam.ai.document.dto.AiPageFragment;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.document.dto.AiQuestionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * QuestionAnalysisParser 类，当前分层的业务组件，负责本模块对应的请求、服务或数据模型职责。
 */
@Service
public class QuestionAnalysisParser {

    private static final Set<String> TYPES = Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER");
    private static final Set<String> PAGE_TYPES = Set.of("QUESTION", "ANSWER_EXPLANATION", "MIXED", "NO_QUESTION");
    private static final Set<String> FRAGMENT_TYPES = Set.of("QUESTION_STEM", "ANSWER", "EXPLANATION", "MIXED");

    private final ObjectMapper objectMapper;

    /**
     * 构造 QuestionAnalysisParser 实例并注入运行所需依赖。
     * @param objectMapper 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionAnalysisParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param rawJson 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public AiQuestionResult parse(String rawJson) {
        try {
            String json = extractJson(rawJson);
            AiQuestionResult result = objectMapper.readValue(json, AiQuestionResult.class);
            validate(result);
            return result;
        } catch (Exception ex) {
            throw BusinessException.badRequest("AI 分析结果格式不合法");
        }
    }

    /**
     * 解析 AI 单页分析结果，允许空片段页面通过，用于答案页或无题页面后续合并。
     *
     * @param rawJson AI 单页分析 JSON。
     * @return 单页分析结果。
     * @throws com.exam.ai.common.exception.BusinessException 当 JSON 格式或页面类型非法时抛出。
     */
    public AiPageAnalysisResult parsePageAnalysis(String rawJson) {
        try {
            String json = extractJson(rawJson);
            AiPageAnalysisResult result = objectMapper.readValue(json, AiPageAnalysisResult.class);
            validatePageAnalysis(result);
            return result;
        } catch (Exception ex) {
            throw BusinessException.badRequest("AI 页级分析结果格式不合法");
        }
    }

    /**
     * 解析 AI 单页分析结果，并校验返回页码必须与当前 chunk 页码一致。
     *
     * <p>该方法用于页级识别成功落库前的强校验。只有当顶层 {@code pageNo} 和每个片段的
     * {@code pageNo} 都等于当前 PDF 页码时，调用方才允许将 raw_json 标记为成功。</p>
     *
     * @param rawJson AI 单页分析 JSON。
     * @param expectedPageNo 当前 chunk 对应的 PDF 页码，从 1 开始。
     * @return 单页分析结果。
     * @throws com.exam.ai.common.exception.BusinessException 当 JSON 格式、页面类型或页码不一致时抛出。
     */
    public AiPageAnalysisResult parsePageAnalysis(String rawJson, Integer expectedPageNo) {
        try {
            String json = extractJson(rawJson);
            AiPageAnalysisResult result = objectMapper.readValue(json, AiPageAnalysisResult.class);
            validatePageAnalysis(result);
            validateExpectedPageNo(result, expectedPageNo);
            return result;
        } catch (Exception ex) {
            throw BusinessException.badRequest("AI 页级分析结果格式不合法");
        }
    }

    /**
     * 解析 AI 文档级合并结果，允许最终题目为空，避免答案页或空文档阻断后处理。
     *
     * @param rawJson AI 文档级合并 JSON。
     * @return 文档级合并结果。
     * @throws com.exam.ai.common.exception.BusinessException 当 JSON 格式或题目字段非法时抛出。
     */
    public AiDocumentAssembleResult parseAssembleResult(String rawJson) {
        try {
            String json = extractJson(rawJson);
            AiDocumentAssembleResult result = objectMapper.readValue(json, AiDocumentAssembleResult.class);
            validateAssembleResult(result);
            return result;
        } catch (Exception ex) {
            throw BusinessException.badRequest("AI 文档合并结果格式不合法");
        }
    }

    /**
     * 校验业务参数或业务状态，阻止非法流程继续执行。
     * @param result 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public void validate(AiQuestionResult result) {
        if (result == null || result.questions() == null || result.questions().isEmpty()) {
            throw BusinessException.badRequest("AI 未识别到题目");
        }
        for (AiQuestionItem item : result.questions()) {
            if (item.type() == null || !TYPES.contains(item.type())) {
                throw BusinessException.badRequest("AI 返回了未知题型");
            }
            if (isBlank(item.stem()) || isBlank(item.standardAnswer())) {
                throw BusinessException.badRequest("AI 返回题目缺少题干或标准答案");
            }
            if (item.difficultyStars() == null || item.difficultyStars() < 1 || item.difficultyStars() > 5) {
                throw BusinessException.badRequest("AI 返回难度星数不合法");
            }
            BigDecimal confidence = item.confidence();
            if (confidence != null && (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) {
                throw BusinessException.badRequest("AI 返回置信度不合法");
            }
        }
    }

    /**
     * 校验 AI 页级分析结构，空页和答案解析页可以没有片段。
     *
     * @param result AI 页级分析结果。
     */
    public void validatePageAnalysis(AiPageAnalysisResult result) {
        if (result == null || result.pageNo() == null || result.pageNo() < 1) {
            throw BusinessException.badRequest("AI 页级分析缺少页码");
        }
        if (result.pageType() == null || !PAGE_TYPES.contains(result.pageType())) {
            throw BusinessException.badRequest("AI 页级分析返回了未知页面类型");
        }
        if (result.fragments() == null) {
            throw BusinessException.badRequest("AI 页级分析缺少片段列表");
        }
        for (AiPageFragment fragment : result.fragments()) {
            if (fragment.pageNo() == null || fragment.pageNo() < 1) {
                throw BusinessException.badRequest("AI 页级片段缺少页码");
            }
            if (fragment.fragmentType() == null || !FRAGMENT_TYPES.contains(fragment.fragmentType())) {
                throw BusinessException.badRequest("AI 页级片段返回了未知片段类型");
            }
            if (fragment.options() == null || fragment.complete() == null || fragment.continuesPreviousQuestion() == null) {
                throw BusinessException.badRequest("AI 页级片段缺少必要字段");
            }
        }
    }

    /**
     * 校验 AI 返回页码与当前处理页一致，避免模型串页导致来源页码与 chunk 关联错乱。
     *
     * @param result AI 页级分析结果。
     * @param expectedPageNo 当前 chunk 对应的 PDF 页码。
     */
    private void validateExpectedPageNo(AiPageAnalysisResult result, Integer expectedPageNo) {
        if (expectedPageNo == null || expectedPageNo < 1) {
            throw BusinessException.badRequest("当前页码不合法");
        }
        if (!Objects.equals(result.pageNo(), expectedPageNo)) {
            throw BusinessException.badRequest("AI 页级分析页码与当前页不一致");
        }
        for (AiPageFragment fragment : result.fragments()) {
            if (!Objects.equals(fragment.pageNo(), expectedPageNo)) {
                throw BusinessException.badRequest("AI 页级片段页码与当前页不一致");
            }
        }
    }

    /**
     * 校验 AI 文档级合并结构，题目列表允许为空。
     *
     * @param result AI 文档级合并结果。
     */
    public void validateAssembleResult(AiDocumentAssembleResult result) {
        if (result == null || result.questions() == null) {
            throw BusinessException.badRequest("AI 文档合并结果缺少题目列表");
        }
        for (AiAssembledQuestionItem item : result.questions()) {
            if (item.type() == null || !TYPES.contains(item.type())) {
                throw BusinessException.badRequest("AI 文档合并返回了未知题型");
            }
            if (isBlank(item.stem()) || isBlank(item.standardAnswer())) {
                throw BusinessException.badRequest("AI 文档合并题目缺少题干或标准答案");
            }
            if (item.options() == null || item.sourcePageNos() == null || item.sourcePageNos().isEmpty()) {
                throw BusinessException.badRequest("AI 文档合并题目缺少选项或来源页码");
            }
            if (item.difficultyStars() == null || item.difficultyStars() < 1 || item.difficultyStars() > 5) {
                throw BusinessException.badRequest("AI 文档合并难度星数不合法");
            }
            BigDecimal confidence = item.confidence();
            if (confidence != null && (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0)) {
                throw BusinessException.badRequest("AI 文档合并置信度不合法");
            }
        }
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param raw 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private String extractJson(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        int fenceStart = trimmed.indexOf("```");
        if (fenceStart >= 0) {
            int contentStart = trimmed.indexOf('\n', fenceStart);
            int fenceEnd = trimmed.lastIndexOf("```");
            if (contentStart > 0 && fenceEnd > contentStart) {
                trimmed = trimmed.substring(contentStart + 1, fenceEnd).trim();
            }
        }
        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    /**
     * 执行当前业务步骤，并返回调用方需要的处理结果。
     * @param value 调用方传入的业务数据，方法会按场景用于校验、查询或状态变更。
     * @return 封装后的业务处理结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
