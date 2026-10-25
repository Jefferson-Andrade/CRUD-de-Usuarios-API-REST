package com.example.crud.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Validate production settings before datasource beans are created, without logging values. */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
public class ProductionConfiguration {

    @Bean
    static BeanFactoryPostProcessor validateProductionEnvironment(Environment environment) {
        return beanFactory -> {
            if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
                throw new IllegalStateException("Nao combine os perfis dev e prod");
            }
            List<String> missing = new ArrayList<>();
            for (String key : List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD")) {
                String value = environment.getProperty(key);
                if (value == null || value.isBlank()) {
                    missing.add(key);
                }
            }
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                        "Perfil prod exige variaveis nao vazias: " + String.join(", ", missing));
            }
        };
    }
}
