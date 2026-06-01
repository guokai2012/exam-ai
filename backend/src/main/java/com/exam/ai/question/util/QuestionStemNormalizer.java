package com.exam.ai.question.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

/**
 * QuestionStemNormalizer 类，承载当前分层中的业务职责。
 */
@Service
public class QuestionStemNormalizer {

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param stem 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
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

    /**
     * 查询或解析业务数据，返回前端或内部流程需要的结果。
     * @param normalizedStem 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public String hash(String normalizedStem) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(normalizedStem.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
