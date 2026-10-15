package com.serviceflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Springdoc OpenAPI 配置：接口文档地址 /swagger-ui.html，JSON 地址 /v3/api-docs。
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI serviceFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceFlow 企业售后工单管理系统 API")
                        .description("售后工单管理系统的后端接口文档，统一前缀 /api/v1")
                        .version("v1.0.0")
                        .contact(new Contact().name("ServiceFlow Team"))
                        .license(new License().name("MIT")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
