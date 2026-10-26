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
                new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"))
                .addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT")))
                .info(new Info()
                .title("CRUD de Usuários API")
                .version("1.0.0")
                .description("API REST para gerenciamento de usuários. Use Authorize com HTTP Basic ou JWT Bearer. "
                        + "READER pode consultar; criação, atualização e exclusão exigem ADMIN e token CSRF durante a transição. "
                        + "Em demonstrações, use somente dados fictícios e consulte o README para acesso."));
    }
}
