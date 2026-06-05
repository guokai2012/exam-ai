package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.dto.AiDocumentAssembleResult;
import com.exam.ai.document.dto.AiPageAnalysisResult;
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

    @Test
    void parsesNoQuestionPageAnalysis() {
        String json = """
                {"pageNo":2,"pageType":"NO_QUESTION","fragments":[]}
                """;

        AiPageAnalysisResult result = parser.parsePageAnalysis(json);

        assertThat(result.pageNo()).isEqualTo(2);
        assertThat(result.pageType()).isEqualTo("NO_QUESTION");
        assertThat(result.fragments()).isEmpty();
    }

    @Test
    void parsesAnswerExplanationFragmentForPreviousQuestion() {
        String json = """
                {"pageNo":3,"pageType":"ANSWER_EXPLANATION","fragments":[{"pageNo":3,"fragmentType":"EXPLANATION","questionNo":"1","stemFragment":"","options":[],"answerFragment":"A","explanationFragment":"详细解析","complete":false,"continuesPreviousQuestion":true}]}
                """;

        AiPageAnalysisResult result = parser.parsePageAnalysis(json);

        assertThat(result.fragments()).hasSize(1);
        assertThat(result.fragments().get(0).continuesPreviousQuestion()).isTrue();
        assertThat(result.fragments().get(0).explanationFragment()).isEqualTo("详细解析");
    }

    @Test
    void parsesEmptyAssembleResult() {
        String json = """
                {"questions":[]}
                """;

        AiDocumentAssembleResult result = parser.parseAssembleResult(json);

        assertThat(result.questions()).isEmpty();
    }

    @Test
    void rejectsAssembledQuestionWithoutSourcePages() {
        String json = """
                {"questions":[{"type":"SINGLE_CHOICE","stem":"Java 是什么？","options":["A. 语言"],"standardAnswer":"A","explanation":"解析","difficultyStars":2,"confidence":0.9,"categoryName":"Java 基础","sourcePageNos":[]}]}
                """;

        assertThatThrownBy(() -> parser.parseAssembleResult(json)).isInstanceOf(BusinessException.class);
    }
}
