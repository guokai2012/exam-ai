package com.exam.ai.document.service.impl;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiPageAnalysisResult;
import com.exam.ai.document.service.DocumentQuestionAssemblerClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 基于 OpenAI-compatible Chat Completions 协议的文档级题目合并客户端。
 */
@Service
public class OpenAiDocumentQuestionAssemblerClient implements DocumentQuestionAssemblerClient {

    private static final String PLACEHOLDER_API_KEY = "sk-placeholder";
    private static final int DEFAULT_MAX_COMPLETION_TOKENS = 8192;
    private static final String RESPONSE_FORMAT_TYPE = "json_schema";
    private static final String RESPONSE_SCHEMA_NAME = "document_assembled_questions";
    private static final String RESPONSE_EMPTY_MESSAGE = "AI 文档合并结果为空";
    private static final String API_KEY_MISSING_MESSAGE = "AI API Key 未配置";
    private static final String ASSEMBLE_PROMPT = """
            你是试卷题目整理助手。输入是按页码升序排列的 PDF 页级片段 JSON。
            请将跨页题干、选项、答案和详细解析合并为完整题目。
            ANSWER_EXPLANATION 且 continuesPreviousQuestion=true 的片段，应优先并入前一题的 standardAnswer 或 explanation。
            NO_QUESTION 页面不生成题目。
            孤立且无法关联到任何题目的答案或解析片段不要单独生成题目。
            sourcePageNos 必须列出组成该题目的全部来源页码，按升序去重。
            如果整篇文档没有完整题目，返回 {"questions":[]}。
            只返回 JSON，不要 Markdown，不要代码块。
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;

    /**
     * 构造 OpenAI-compatible 文档级题目合并客户端。
     *
     * @param restClientBuilder Spring RestClient 构建器。
     * @param objectMapper JSON 序列化工具。
     * @param baseUrl Spring AI OpenAI-compatible 基础地址。
     * @param apiKey Spring AI OpenAI-compatible API Key。
     * @param modelName Spring AI OpenAI-compatible 模型名。
     */
    public OpenAiDocumentQuestionAssemblerClient(RestClient.Builder restClientBuilder,
                                                 ObjectMapper objectMapper,
                                                 @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
                                                 @Value("${spring.ai.openai.api-key:}") String apiKey,
                                                 @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String modelName) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    /**
     * 调用文档级合并模型，将页级片段整理成完整题目。
     *
     * @param pages 按页码升序排列的页级分析结果。
     * @return 模型返回的完整题目 JSON。
     */
    @Override
    public String assembleQuestions(List<AiPageAnalysisResult> pages) {
        validateApiKey();
        String pageJson = pageAnalysisJson(pages);
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", ASSEMBLE_PROMPT + "\n页级片段 JSON：\n" + pageJson
                )),
                "max_tokens", DEFAULT_MAX_COMPLETION_TOKENS,
                "response_format", responseFormat()
        );
        try {
            Map<?, ?> response = restClient.post()
                    .uri(chatCompletionsEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);
            String content = extractContent(response);
            if (content == null || content.isBlank()) {
                throw BusinessException.badRequest(RESPONSE_EMPTY_MESSAGE);
            }
            return content;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw BusinessException.badRequest("AI 文档合并调用失败：" + ex.getMessage());
        }
    }

    /**
     * 校验 API Key，避免使用占位配置发起无效外部调用。
     */
    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank() || PLACEHOLDER_API_KEY.equals(apiKey)) {
            throw BusinessException.badRequest(API_KEY_MISSING_MESSAGE);
        }
    }

    /**
     * 序列化页级分析结果。
     *
     * @param pages 页级分析结果。
     * @return JSON 字符串。
     */
    private String pageAnalysisJson(List<AiPageAnalysisResult> pages) {
        try {
            return objectMapper.writeValueAsString(pages == null ? List.of() : pages);
        } catch (JsonProcessingException ex) {
            throw BusinessException.badRequest("页级片段序列化失败");
        }
    }

    /**
     * 构造 OpenAI Structured Outputs 配置，要求模型严格返回完整题目 JSON 结构。
     *
     * @return Chat Completions {@code response_format} 请求参数。
     */
    private Map<String, Object> responseFormat() {
        return Map.of(
                "type", RESPONSE_FORMAT_TYPE,
                "json_schema", Map.of(
                        "name", RESPONSE_SCHEMA_NAME,
                        "strict", true,
                        "schema", assembleResultSchema()
                )
        );
    }

    /**
     * 构造文档级合并结果 JSON Schema。
     *
     * @return 严格结构化输出使用的 JSON Schema。
     */
    private Map<String, Object> assembleResultSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("questions"),
                "properties", Map.of(
                        "questions", Map.of(
                                "type", "array",
                                "items", assembledQuestionSchema()
                        )
                )
        );
    }

    /**
     * 构造完整题目 JSON Schema。
     *
     * @return 完整题目对象 JSON Schema。
     */
    private Map<String, Object> assembledQuestionSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("type", "stem", "options", "standardAnswer", "explanation",
                        "difficultyStars", "confidence", "categoryName", "sourcePageNos"),
                "properties", Map.of(
                        "type", Map.of(
                                "type", "string",
                                "enum", List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE", "TRUE_FALSE", "SHORT_ANSWER")
                        ),
                        "stem", Map.of("type", "string"),
                        "options", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        ),
                        "standardAnswer", Map.of("type", "string"),
                        "explanation", Map.of("type", "string"),
                        "difficultyStars", Map.of("type", "integer"),
                        "confidence", Map.of("type", "number"),
                        "categoryName", Map.of("type", "string"),
                        "sourcePageNos", Map.of(
                                "type", "array",
                                "items", Map.of("type", "integer")
                        )
                )
        );
    }

    /**
     * 解析 OpenAI-compatible Chat Completions 响应中的首个消息内容。
     *
     * @param response 接口原始响应 Map。
     * @return 首个 choice 的 message.content。
     */
    private String extractContent(Map<?, ?> response) {
        if (response == null) {
            return null;
        }
        Object choicesObject = response.get("choices");
        if (!(choicesObject instanceof List<?> choices) || choices.isEmpty()) {
            return null;
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choiceMap)) {
            return null;
        }
        Object messageObject = choiceMap.get("message");
        if (!(messageObject instanceof Map<?, ?> messageMap)) {
            return null;
        }
        Object content = messageMap.get("content");
        return content == null ? null : String.valueOf(content);
    }

    /**
     * 根据 Spring AI OpenAI base-url 计算 Chat Completions 完整地址。
     *
     * @return OpenAI-compatible chat completions URL。
     */
    private String chatCompletionsEndpoint() {
        String normalized = baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com" : baseUrl.stripTrailing();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith("/v1")) {
            return normalized + "/chat/completions";
        }
        return normalized + "/v1/chat/completions";
    }
}
