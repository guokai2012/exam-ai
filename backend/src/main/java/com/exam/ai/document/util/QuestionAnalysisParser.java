package com.exam.ai.document.util;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiQuestionItem;
import com.exam.ai.document.dto.AiQuestionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * QuestionAnalysisParser 类，承载当前分层中的业务职责。
 */
@Service
public class QuestionAnalysisParser {

    private static final Set<String> TYPES = Set.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER");

    private final ObjectMapper objectMapper;

    /**
     * 构造 QuestionAnalysisParser 实例并注入运行所需依赖。
     * @param objectMapper 业务参数，参与当前方法的校验、查询或状态变更。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public QuestionAnalysisParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param rawJson 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
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
     * 校验业务参数或状态，阻止非法流程继续执行。
     * @param result 业务参数，参与当前方法的校验、查询或状态变更。
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
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param raw 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
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
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param value 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
