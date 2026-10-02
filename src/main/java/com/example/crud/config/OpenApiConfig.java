package com.example.crud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI crudOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("CRUD de Usuários API")
                .version("1.0.0")
                .description("API REST para gerenciamento de usuários."));
    }
}
