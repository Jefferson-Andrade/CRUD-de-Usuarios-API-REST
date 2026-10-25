package com.example.crud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI crudOpenAPI() {
        return new OpenAPI().components(new Components().addSecuritySchemes("basicAuth",
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .info(new Info()
                .title("CRUD de Usuários API")
                .version("1.0.0")
                .description("API REST para gerenciamento de usuários."));
    }
}
