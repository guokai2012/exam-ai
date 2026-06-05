package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.exam.ai.document.dto.AiPageAnalysisResult;
import com.exam.ai.document.dto.AiPageFragment;
import com.exam.ai.document.service.impl.OpenAiDocumentQuestionAssemblerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiDocumentQuestionAssemblerClientTest {

    @Test
    void sendsAssembledQuestionJsonSchema() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenAiDocumentQuestionAssemblerClient client = new OpenAiDocumentQuestionAssemblerClient(
                restClientBuilder,
                new ObjectMapper(),
                "https://api.openai.com/v1",
                "sk-test",
                "gpt-4o-mini"
        );
        AiPageAnalysisResult page = new AiPageAnalysisResult(1, "ANSWER_EXPLANATION", List.of(
                new AiPageFragment(1, "EXPLANATION", "1", "", List.of(), "A", "解析内容", false, true)
        ));

        server.expect(once(), requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.extra_body.thinking.type").value("disabled"))
                .andExpect(jsonPath("$.extra_body.reasoning_split").value(false))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("document_assembled_questions"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.required[0]").value("questions"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.questions.items.properties.sourcePageNos.type").value("array"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.questions.items.properties.type.enum[0]").value("SINGLE_CHOICE"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"questions\\\":[]}\"}}]}",
                        MediaType.APPLICATION_JSON));

        String result = client.assembleQuestions(List.of(page));

        assertThat(result).isEqualTo("{\"questions\":[]}");
        server.verify();
    }
}
