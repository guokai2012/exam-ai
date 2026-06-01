package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.exam.ai.document.util.DocumentChunker;
import java.util.List;
import org.junit.jupiter.api.Test;

class DocumentChunkerTest {

    private final DocumentChunker chunker = new DocumentChunker();

    @Test
    void shouldKeepQuestionAnswerAndExplanationInSameChunk() {
        String text = """
                1. Java 是什么？
                A. 编程语言
                答案：A
                解析：Java 是一种编程语言。

                2. Redis 常用数据结构有哪些？
                答案：String、Hash、List。
                解析：Redis 支持多种数据结构。
                """;

        List<DocumentChunker.DocumentChunk> chunks = chunker.chunk(text, 45);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).chunkText()).contains("1. Java 是什么？", "答案：A", "解析：Java 是一种编程语言。");
        assertThat(chunks.get(1).chunkText()).contains("2. Redis 常用数据结构有哪些？", "String、Hash、List");
    }

    @Test
    void shouldNotHardSplitOversizedSingleQuestion() {
        String text = "第1题 " + "很长".repeat(600) + "\n答案：略";

        List<DocumentChunker.DocumentChunk> chunks = chunker.chunk(text, 100);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).oversized()).isTrue();
        assertThat(chunks.get(0).chunkText()).contains("第1题", "答案：略");
    }

    @Test
    void shouldKeepUnnumberedDocumentAsSingleChunk() {
        String text = "这是一段没有明显题号的资料。\n\n答案可能散落在文本里。";

        List<DocumentChunker.DocumentChunk> chunks = chunker.chunk(text, 10);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).chunkText()).isEqualTo(text);
    }
}
