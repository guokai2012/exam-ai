package com.exam.ai.question.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuestionStemNormalizerTest {

    private final QuestionStemNormalizer normalizer = new QuestionStemNormalizer();

    @Test
    void normalizesQuestionNumberAndPunctuation() {
        String first = normalizer.normalize("1. Java 中 String 是否可变？");
        String second = normalizer.normalize("【判断题】Java 中 String 是否可变?");

        assertThat(first).isEqualTo(second);
        assertThat(normalizer.hash(first)).isEqualTo(normalizer.hash(second));
    }

    @Test
    void keepsMathVariantsDifferentWhenNumbersDiffer() {
        String first = normalizer.normalize("鸡兔同笼，10只头和20只脚。");
        String second = normalizer.normalize("鸡兔同笼，30只头和60只脚。");

        assertThat(first).isNotEqualTo(second);
        assertThat(normalizer.hash(first)).isNotEqualTo(normalizer.hash(second));
    }
}
