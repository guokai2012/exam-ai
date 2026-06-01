package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiQuestionResult;
import com.exam.ai.document.util.QuestionAnalysisParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class QuestionAnalysisParserTest {

    private final QuestionAnalysisParser parser = new QuestionAnalysisParser(new ObjectMapper());

    @Test
    void parsesValidAiJson() {
        String json = """
                {"questions":[{"type":"SINGLE_CHOICE","stem":"Java 是什么？","options":["A. 语言"],"standardAnswer":"A","explanation":"Java 是编程语言","difficultyStars":2,"confidence":0.9,"categoryName":"Java 基础"}]}
                """;

        AiQuestionResult result = parser.parse(json);

        assertThat(result.questions()).hasSize(1);
        assertThat(result.questions().get(0).difficultyStars()).isEqualTo(2);
        assertThat(result.questions().get(0).categoryName()).isEqualTo("Java 基础");
    }

    @Test
    void rejectsInvalidDifficultyStars() {
        String json = """
                {"questions":[{"type":"SINGLE_CHOICE","stem":"Java 是什么？","options":[],"standardAnswer":"A","explanation":"","difficultyStars":6,"confidence":0.9,"categoryName":"Java 基础"}]}
                """;

        assertThatThrownBy(() -> parser.parse(json)).isInstanceOf(BusinessException.class);
    }
}
