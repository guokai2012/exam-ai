package com.exam.ai.document.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * DocumentChunker 类，承载当前分层中的业务职责。
 */
@Component
public class DocumentChunker {

    private static final Pattern QUESTION_START = Pattern.compile(
            "^\\s*((\\d{1,3}[.、．])|([一二三四五六七八九十百]+[、.．])|(\\([一二三四五六七八九十百]+\\))|(（[一二三四五六七八九十百]+）)|(第\\s*\\d+\\s*题)|(题目\\s*\\d+)|(【?(单选题|多选题|判断题|简答题)】?[:：]?)).*"
    );

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param text 业务参数，参与当前方法的校验、查询或状态变更。
     * @param maxChars 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    public List<DocumentChunk> chunk(String text, int maxChars) {
        String source = text == null ? "" : text;
        int target = Math.max(1, maxChars);
        List<QuestionBlock> questions = questionBlocks(source);
        if (questions.isEmpty()) {
            return List.of(toChunk(source, 0, 0, source.length(), 1, source.length() > target));
        }
        List<DocumentChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int chunkStart = questions.get(0).startOffset();
        int questionCount = 0;
        for (QuestionBlock question : questions) {
            if (!current.isEmpty() && current.length() + question.text().length() > target) {
                chunks.add(toChunk(current.toString(), chunks.size(), chunkStart, question.startOffset(), questionCount, current.length() > target));
                current.setLength(0);
                chunkStart = question.startOffset();
                questionCount = 0;
            }
            current.append(question.text());
            questionCount++;
            if (question.text().length() > target) {
                chunks.add(toChunk(current.toString(), chunks.size(), chunkStart, question.endOffset(), questionCount, true));
                current.setLength(0);
                questionCount = 0;
            }
        }
        if (!current.isEmpty()) {
            chunks.add(toChunk(current.toString(), chunks.size(), chunkStart, source.length(), questionCount, current.length() > target));
        }
        return chunks;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param text 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private List<QuestionBlock> questionBlocks(String text) {
        List<Line> lines = lines(text);
        List<QuestionBlock> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int start = -1;
        int end = 0;
        boolean seenQuestion = false;
        for (Line line : lines) {
            boolean startLine = QUESTION_START.matcher(line.text().stripTrailing()).matches();
            if (startLine) {
                seenQuestion = true;
                if (!current.isEmpty()) {
                    blocks.add(new QuestionBlock(current.toString(), start, end));
                    current.setLength(0);
                }
                start = line.startOffset();
            }
            if (seenQuestion) {
                if (start < 0) {
                    start = line.startOffset();
                }
                current.append(line.text());
                end = line.endOffset();
            }
        }
        if (!current.isEmpty()) {
            blocks.add(new QuestionBlock(current.toString(), start, end));
        }
        return blocks;
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param text 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private List<Line> lines(String text) {
        List<Line> lines = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines.add(new Line(text.substring(start, i + 1), start, i + 1));
                start = i + 1;
            }
        }
        if (start < text.length()) {
            lines.add(new Line(text.substring(start), start, text.length()));
        }
        return lines;
    }

    /**
     * 转换业务对象，生成前端返回视图或内部传输结构。
     * @param text 业务参数，参与当前方法的校验、查询或状态变更。
     * @param index 业务参数，参与当前方法的校验、查询或状态变更。
     * @param startOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @param endOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @param questionCount 业务参数，参与当前方法的校验、查询或状态变更。
     * @param oversized 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private DocumentChunk toChunk(String text, int index, int startOffset, int endOffset, int questionCount, boolean oversized) {
        return new DocumentChunk(index, text, sha256(text), startOffset, endOffset, questionCount, oversized);
    }

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @param value 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    /**
     * QuestionBlock 记录对象，封装当前业务流程中的不可变数据。
     * @param text 业务参数，参与当前方法的校验、查询或状态变更。
     * @param startOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @param endOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private record QuestionBlock(String text, int startOffset, int endOffset) {
    }

    /**
     * Line 记录对象，封装当前业务流程中的不可变数据。
     * @param text 业务参数，参与当前方法的校验、查询或状态变更。
     * @param startOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @param endOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    private record Line(String text, int startOffset, int endOffset) {
    }

    /**
     * DocumentChunk 记录对象，封装当前业务流程中的不可变数据。
     * @param chunkIndex 业务参数，参与当前方法的校验、查询或状态变更。
     * @param chunkText 业务参数，参与当前方法的校验、查询或状态变更。
     * @param chunkHash 业务参数，参与当前方法的校验、查询或状态变更。
     * @param startOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @param endOffset 业务参数，参与当前方法的校验、查询或状态变更。
     * @param questionCountEstimate 业务参数，参与当前方法的校验、查询或状态变更。
     * @param oversized 业务参数，参与当前方法的校验、查询或状态变更。
     * @return 当前业务步骤的处理结果。
     */
    public record DocumentChunk(
            int chunkIndex,
            String chunkText,
            String chunkHash,
            int startOffset,
            int endOffset,
            int questionCountEstimate,
            boolean oversized
    ) {
    }
}
