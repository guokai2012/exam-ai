package com.exam.ai.document.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.exam.ai.document.dto.RetryFailedPagesRequest;
import com.exam.ai.document.service.impl.DocumentServiceImpl;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class DocumentServiceImplTransactionTest {

    @Test
    void pageRecognitionEntrypointsAreNotTransactional() throws Exception {
        Method analyze = DocumentServiceImpl.class.getMethod("analyze", Long.class);
        Method retryFailedPages = DocumentServiceImpl.class.getMethod(
                "retryFailedPages", Long.class, RetryFailedPagesRequest.class);

        assertThat(analyze.getAnnotation(Transactional.class)).isNull();
        assertThat(retryFailedPages.getAnnotation(Transactional.class)).isNull();
    }
}
