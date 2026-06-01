package com.exam.ai.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenApiConfig 类，承载当前分层中的业务职责。
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    /**
     * 执行当前业务步骤，维护调用方需要的处理结果。
     * @return 当前业务步骤的处理结果。
     * @throws com.exam.ai.common.exception.BusinessException 当参数非法、资源不存在或业务状态不允许继续处理时抛出。
     */
    @Bean
    public OpenAPI examAiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Exam AI 后台接口文档")
                        .version("1.0.0")
                        .description("智能试题生成与后台管理系统接口文档，统一返回 ApiResponse 包装结构。")
                        .contact(new Contact().name("Exam AI")))
                .components(new Components().addSecuritySchemes(BEARER_AUTH,
                        new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
