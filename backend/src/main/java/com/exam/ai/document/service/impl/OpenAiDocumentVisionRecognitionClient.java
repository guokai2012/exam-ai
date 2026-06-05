package com.exam.ai.document.service.impl;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.service.DocumentVisionRecognitionClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 基于 OpenAI-compatible Chat Completions 协议的 PDF 页图片识别客户端。
 *
 * <p>base-url、api-key 和模型名全部复用 {@code spring.ai.openai.*}，不引入新的 AI 配置项。
 * 请求使用 base64 data URL 传递页面图片，避免必须把本地页面图片暴露成公网 URL。</p>
 */
@Service
public class OpenAiDocumentVisionRecognitionClient implements DocumentVisionRecognitionClient {

    private static final String PLACEHOLDER_API_KEY = "sk-placeholder";
    private static final int DEFAULT_MAX_COMPLETION_TOKENS = 4096;
    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final String IMAGE_MIME_TYPE = "image/png";
    private static final String RESPONSE_FORMAT_TYPE = "json_schema";
    private static final String RESPONSE_SCHEMA_NAME = "document_page_fragments";
    private static final String RESPONSE_EMPTY_MESSAGE = "AI 页面识别结果为空";
    private static final String API_KEY_MISSING_MESSAGE = "AI API Key 未配置";
    private static final String PAGE_PROMPT = """
            你是试卷 PDF 页面识别助手。请只识别当前页面图片中出现的题目内容，不要编造页面外内容。
            返回当前页的页面类型和片段，不要直接把跨页题目合并成完整题。
            pageType 只能是 QUESTION、ANSWER_EXPLANATION、MIXED、NO_QUESTION。
            如果当前页只有答案或详细解析，pageType 使用 ANSWER_EXPLANATION，continuesPreviousQuestion 为 true。
            如果当前页没有题目、答案或解析，pageType 使用 NO_QUESTION，fragments 返回空数组。
            对图片、表格、公式题，请在 stemFragment 或 explanationFragment 中用文字说明视觉内容。
            所有字段必须严格符合 JSON Schema：顶层必须有 pageNo、pageType、fragments；每个片段必须有 pageNo、fragmentType、questionNo、stemFragment、options、answerFragment、explanationFragment、complete、continuesPreviousQuestion。
            没有内容的字符串字段必须返回空字符串 ""，没有选项必须返回空数组 []，不要省略必填字段。
            正向示例：
            {
              "pageNo": 3,
              "pageType": "MIXED",
              "fragments": [
                {
                  "pageNo": 3,
                  "fragmentType": "MIXED",
                  "questionNo": "1",
                  "stemFragment": "Java 线程池如何顺序执行任务？",
                  "options": [],
                  "answerFragment": "可使用单线程线程池或在提交侧保证任务依赖顺序。",
                  "explanationFragment": "单线程线程池天然按队列顺序执行；复杂场景可通过 CompletableFuture 串联任务。",
                  "complete": true,
                  "continuesPreviousQuestion": false
                }
              ]
            }
            反向示例，以下格式禁止返回：
            {"pageType":"MIXED","pageNumber":3,"fragments":[{"type":"QUESTION","content":"题目文本","title":"标题","id":"q1"}]}
            禁止使用 type、content、pageNumber、title、id 等非 Schema 字段，禁止缺少 pageNo，禁止用 fragmentType 之外的片段类型字段。
            只返回 JSON，不要 Markdown，不要代码块。
            """;

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiKey;
    private final String modelName;

    /**
     * 构造 OpenAI-compatible 视觉识别客户端。
     *
     * @param restClientBuilder Spring RestClient 构建器。
     * @param baseUrl           Spring AI OpenAI-compatible 基础地址。
     * @param apiKey            Spring AI OpenAI-compatible API Key。
     * @param modelName         Spring AI OpenAI-compatible 模型名。
     */
    public OpenAiDocumentVisionRecognitionClient(RestClient.Builder restClientBuilder,
                                                 @Value("${spring.ai.openai.base-url:https://api.openai.com}") String baseUrl,
                                                 @Value("${spring.ai.openai.api-key:}") String apiKey,
                                                 @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String modelName) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    /**
     * 调用 OpenAI-compatible 多模态接口识别单页图片。
     *
     * @param pageImagePath PDF 页渲染后的图片本地路径。
     * @param pageNo        页码，从 1 开始，用于提示模型和排查失败。
     * @return 模型返回的原始 JSON 字符串。
     * @throws BusinessException 当 API Key 缺失、图片读取失败、模型调用失败或响应为空时抛出。
     */
    @Override
    public String recognizePage(Path pageImagePath, Integer pageNo) {
        validateApiKey();
        String dataUrl = imageDataUrl(pageImagePath);
        Map<String, Object> requestBody = Map.of(
                "model", modelName,
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of("type", "text", "text", "当前是 PDF 第 " + pageNo + " 页。\n" + PAGE_PROMPT),
                                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                        )
                )),
                "max_tokens", DEFAULT_MAX_COMPLETION_TOKENS,
                "temperature", DEFAULT_TEMPERATURE,
                "thinking", Map.of("type", "disabled"),
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
            throw BusinessException.badRequest("AI 页面识别调用失败：" + ex.getMessage());
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
     * 将本地页面图片转换为 OpenAI-compatible image_url 支持的 data URL。
     *
     * @param pageImagePath 页面图片路径。
     * @return base64 data URL。
     */
    private String imageDataUrl(Path pageImagePath) {
        try {
            String encoded = Base64.getEncoder().encodeToString(Files.readAllBytes(pageImagePath));
            return "data:" + IMAGE_MIME_TYPE + ";base64," + encoded;
        } catch (IOException ex) {
            throw BusinessException.badRequest("页面图片读取失败");
        }
    }

    /**
     * 构造 OpenAI Structured Outputs 配置，要求模型严格返回题目 JSON 结构。
     *
     * @return Chat Completions {@code response_format} 请求参数。
     */
    private Map<String, Object> responseFormat() {
        return Map.of(
                "type", RESPONSE_FORMAT_TYPE,
                "json_schema", Map.of(
                        "name", RESPONSE_SCHEMA_NAME,
                        "strict", true,
                        "schema", pageAnalysisSchema()
                )
        );
    }

    /**
     * 构造 MiniMax M3 OpenAI-compatible 扩展参数，关闭深度思考输出。
     *
     * @return Chat Completions {@code extra_body} 请求参数。
     */
    private Map<String, Object> extraBody() {
        return Map.of(
                "thinking", Map.of("type", "disabled"),
                "reasoning_split", false
        );
    }

    /**
     * 构造页级片段识别结果 JSON Schema。
     *
     * @return 严格结构化输出使用的 JSON Schema。
     */
    private Map<String, Object> pageAnalysisSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("pageNo", "pageType", "fragments"),
                "properties", Map.of(
                        "pageNo", Map.of("type", "integer"),
                        "pageType", Map.of(
                                "type", "string",
                                "enum", List.of("QUESTION", "ANSWER_EXPLANATION", "MIXED", "NO_QUESTION")
                        ),
                        "fragments", Map.of(
                                "type", "array",
                                "items", pageFragmentSchema()
                        )
                )
        );
    }

    /**
     * 构造单页片段 JSON Schema。
     *
     * @return 单页片段 JSON Schema。
     */
    private Map<String, Object> pageFragmentSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("pageNo", "fragmentType", "questionNo", "stemFragment", "options",
                        "answerFragment", "explanationFragment", "complete", "continuesPreviousQuestion"),
                "properties", Map.of(
                        "pageNo", Map.of("type", "integer"),
                        "fragmentType", Map.of(
                                "type", "string",
                                "enum", List.of("QUESTION_STEM", "ANSWER", "EXPLANATION", "MIXED")
                        ),
                        "questionNo", Map.of("type", "string"),
                        "stemFragment", Map.of("type", "string"),
                        "options", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string")
                        ),
                        "answerFragment", Map.of("type", "string"),
                        "explanationFragment", Map.of("type", "string"),
                        "complete", Map.of("type", "boolean"),
                        "continuesPreviousQuestion", Map.of("type", "boolean")
                )
        );
    }

    /**
     * 解析 OpenAI-compatible Chat Completions 响应中的首个消息内容。
     *
     * @param response 接口原始响应 Map。
     * @return 首个 choice 的 message.content。
     */
    @SuppressWarnings("unchecked")
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
