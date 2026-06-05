package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.exam.ai.common.config.DocumentProperties;
import com.exam.ai.common.exception.BusinessException;
import com.exam.ai.document.service.impl.DocumentFileServiceImpl;
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

        String filename = service.sanitizeFilename("../试题?.pdf");

        assertThat(filename).isEqualTo("试题_.pdf");
        assertThat(service.extension(filename)).isEqualTo("pdf");
    }

    @Test
    void calculatesPdfSha256AndRejectsNonPdfExtension() throws Exception {
        DocumentProperties properties = new DocumentProperties();
        DocumentFileServiceImpl service = new DocumentFileServiceImpl(properties);
        Path file = Files.createTempFile("exam-ai", ".pdf");
        Files.write(file, "%PDF-1.4".getBytes());

        assertThat(service.sha256(file)).hasSize(64);
        assertThatThrownBy(() -> service.validate(new org.springframework.mock.web.MockMultipartFile(
                "file", "questions.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("仅支持 PDF 文件");
    }
}
