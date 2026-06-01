package com.exam.ai.question.service;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.question.entity.ExamQuestionBank;
import com.exam.ai.system.service.SystemConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class QuestionTaggingService {

    private static final String SYSTEM_PROMPT = """
            你是考试题目标签分析助手。请根据题干、题型、选项和标准答案，为题目生成 2 到 5 个检索标签。
            标签应覆盖题型模式、知识点、能力点，例如：鸡兔同笼、数学应用题、二元一次方程、Java 集合。
            只返回 JSON，不要返回 Markdown 或解释文字。格式必须为：{"tags":["标签1","标签2"]}
            """;

    private final QuestionBankService questionBankService;
    private final SystemConfigService systemConfigService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public QuestionTaggingService(QuestionBankService questionBankService,
                                  SystemConfigService systemConfigService,
                                  ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                  ObjectMapper objectMapper,
                                  @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.questionBankService = questionBankService;
        this.systemConfigService = systemConfigService;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public void tagPendingQuestions() {
        int maxRetries = systemConfigService.aiTaggingMaxRetries();
        for (ExamQuestionBank candidate : questionBankService.tagCandidates(10, maxRetries)) {
            ExamQuestionBank processing = questionBankService.startTagging(candidate);
            try {
                List<String> tags = analyzeTags(processing);
                questionBankService.tagSuccess(processing.getId(), tags);
            } catch (Exception ex) {
                questionBankService.tagFailed(processing.getId(), ex.getMessage(), maxRetries);
            }
        }
    }

    public List<String> analyzeTags(ExamQuestionBank question) {
        if (apiKey == null || apiKey.isBlank() || "sk-placeholder".equals(apiKey)) {
            throw BusinessException.badRequest("AI API Key 未配置");
        }
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw BusinessException.badRequest("AI 客户端未配置");
        }
        String content = """
                题型：%s
                题干：%s
                选项：%s
                标准答案：%s
                解析：%s
                """.formatted(
                question.getQuestionType(),
                question.getStem(),
                question.getOptionsJson(),
                question.getStandardAnswer(),
                question.getExplanation()
        );
        String raw = builder.build().prompt()
                .system(SYSTEM_PROMPT)
                .user(content)
                .call()
                .content();
        return parseTags(raw);
    }

    private List<String> parseTags(String raw) {
        try {
            String json = extractJson(raw);
            TagResult result = objectMapper.readValue(json, TagResult.class);
            if (result.tags() == null || result.tags().isEmpty()) {
                throw BusinessException.badRequest("AI 未返回题型标签");
            }
            return result.tags().stream()
                    .filter(tag -> tag != null && !tag.isBlank())
                    .map(String::trim)
                    .distinct()
                    .limit(5)
                    .toList();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest("AI 标签结果格式不合法");
        }
    }

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

    private record TagResult(List<String> tags) {
    }
}

