package com.exam.ai.question.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class QuestionStemNormalizer {

    public String normalize(String stem) {
        if (stem == null) {
            return "";
        }
        String normalized = Normalizer.normalize(stem, Normalizer.Form.NFKC)
                .replace('？', '?')
                .replace('，', ',')
                .replace('。', '.')
                .replace('；', ';')
                .replace('：', ':')
                .replaceAll("^\\s*(第?[一二三四五六七八九十百千万]+[题、.．)]|\\(?\\d+[).、．]|【[^】]{1,12}题】|\\[[^\\]]{1,12}题]\\s*)", "")
                .replaceAll("^(单选题|多选题|判断题|简答题|选择题)\\s*[:：、.-]*\\s*", "")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    public String hash(String normalizedStem) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedStem.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
