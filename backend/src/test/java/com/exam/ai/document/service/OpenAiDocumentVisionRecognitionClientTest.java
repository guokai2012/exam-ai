package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.exam.ai.document.service.impl.OpenAiDocumentVisionRecognitionClient;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiDocumentVisionRecognitionClientTest {

    @TempDir
    private Path tempDir;

    @Test
    void sendsJsonSchemaResponseFormat() throws Exception {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        OpenAiDocumentVisionRecognitionClient client = new OpenAiDocumentVisionRecognitionClient(
                restClientBuilder,
                "https://api.openai.com/v1",
                "sk-test",
                "gpt-4o-mini"
        );
        Path pageImage = tempDir.resolve("page.png");
        Files.writeString(pageImage, "fake image bytes");

        server.expect(once(), requestTo("https://api.openai.com/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer sk-test"))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.messages[0].content[0].text", containsString("正向示例")))
                .andExpect(jsonPath("$.messages[0].content[0].text", containsString("反向示例")))
                .andExpect(jsonPath("$.messages[0].content[0].text", containsString("禁止使用 type、content、pageNumber、title、id")))
                .andExpect(jsonPath("$.extra_body.thinking.type").value("disabled"))
                .andExpect(jsonPath("$.extra_body.reasoning_split").value(false))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.name").value("document_page_fragments"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.required[0]").value("pageNo"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.additionalProperties").value(false))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.pageType.enum[0]").value("QUESTION"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.fragments.type").value("array"))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.fragments.items.additionalProperties").value(false))
                .andExpect(jsonPath("$.response_format.json_schema.schema.properties.fragments.items.properties.fragmentType.enum[0]").value("QUESTION_STEM"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"{\\\"pageNo\\\":1,\\\"pageType\\\":\\\"NO_QUESTION\\\",\\\"fragments\\\":[]}\"}}]}",
                        MediaType.APPLICATION_JSON));

        String result = client.recognizePage(pageImage, 1);

        assertThat(result).contains("\"pageType\":\"NO_QUESTION\"");
        server.verify();
    }
}
