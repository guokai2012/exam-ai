package com.exam.ai;

import com.exam.ai.common.config.SecurityProperties;
import com.exam.ai.common.config.AiAnalysisProperties;
import com.exam.ai.common.config.DocumentProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ExamAiBackendApplication 类，承载当前分层中的业务职责。
 */
@MapperScan("com.exam.ai.**.mapper")
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({SecurityProperties.class, DocumentProperties.class, AiAnalysisProperties.class})
public class ExamAiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamAiBackendApplication.class, args);
    }
}
