package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.exam.ai.common.config.DocumentProperties;
import com.exam.ai.document.service.impl.DocumentFileServiceImpl;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class DocumentFileServiceTest {

    @Test
    void sanitizesFilenameAndResolvesExtension() {
        DocumentProperties properties = new DocumentProperties();
        properties.setMaxSize(DataSize.ofMegabytes(20));
        DocumentFileServiceImpl service = new DocumentFileServiceImpl(properties);

        String filename = service.sanitizeFilename("../试题?.md");

        assertThat(filename).isEqualTo("试题_.md");
        assertThat(service.extension(filename)).isEqualTo("md");
    }

    @Test
    void extractsMarkdownAndCalculatesSha256() throws Exception {
        DocumentProperties properties = new DocumentProperties();
        DocumentFileServiceImpl service = new DocumentFileServiceImpl(properties);
        Path file = Files.createTempFile("exam-ai", ".md");
        Files.writeString(file, "# 题目\n\n1. Java 是什么？", StandardCharsets.UTF_8);

        assertThat(service.extractText(file, "md")).contains("Java 是什么");
        assertThat(service.sha256(file)).hasSize(64);
    }
}
