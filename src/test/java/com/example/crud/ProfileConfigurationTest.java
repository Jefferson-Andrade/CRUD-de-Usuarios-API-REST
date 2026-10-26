package com.example.crud;

import com.example.crud.config.ProductionConfiguration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.NestedExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileConfigurationTest {
    // Load the real runtime files; test/resources/application.properties is intentionally separate.
    // Ignore host environment/system settings so local credentials cannot make these tests pass.
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> {
                var sources = context.getEnvironment().getPropertySources();
                sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
                sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            })
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(ProductionConfiguration.class)
            .withPropertyValues("spring.config.location=file:./src/main/resources/");

    @Test
    void defaultDevelopmentKeepsLocalDatabaseAndSafeSchemaSettings() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var env = context.getEnvironment();
            assertThat(env.getDefaultProfiles()).containsExactly("dev");
            assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5432/crud");
            assertThat(env.getProperty("spring.datasource.username")).isEqualTo("postgres");
            assertThat(env.getProperty("spring.jpa.show-sql")).isEqualTo("true");
            assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
            assertThat(env.getProperty("spring.flyway.baseline-on-migrate")).isEqualTo("false");
            assertThat(env.getProperty("app.security.bootstrap.password")).isEmpty();
        });
    }

    @Test
    void developmentAcceptsExternalDatabaseSettings() {
        runner.withPropertyValues("spring.profiles.active=dev",
                "DB_URL=jdbc:postgresql://db:5432/demo", "DB_USERNAME=demo", "DB_PASSWORD=test-fixture")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://db:5432/demo");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.username")).isEqualTo("demo");
                    assertThat(context.getEnvironment().getProperty("spring.datasource.password")).isEqualTo("test-fixture");
                });
    }

    @Test
    void productionLoadsExternalSettingsWithoutSqlOrInternalErrorDetails() {
        runner.withPropertyValues(production(null, null)).run(context -> {
            assertThat(context).hasNotFailed();
            var env = context.getEnvironment();
            assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://db:5432/demo");
            assertThat(env.getProperty("spring.datasource.username")).isEqualTo("demo");
            assertThat(env.getProperty("spring.datasource.password")).isEqualTo("test-fixture");
            assertThat(env.getProperty("spring.jpa.show-sql")).isEqualTo("false");
            assertThat(env.getProperty("logging.level.org.hibernate.SQL")).isEqualTo("OFF");
            assertThat(env.getProperty("logging.level.org.hibernate.orm.jdbc.bind")).isEqualTo("OFF");
            assertThat(env.getProperty("server.error.include-stacktrace")).isEqualTo("never");
            assertThat(env.getProperty("server.error.include-message")).isEqualTo("never");
            assertThat(env.getProperty("app.security.bootstrap.username")).isEmpty();
            assertThat(env.getProperty("app.security.bootstrap.password")).isEmpty();
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"DB_URL", "DB_USERNAME", "DB_PASSWORD"})
    void productionRejectsMissingSettingsWithoutRevealingValues(String key) {
        runner.withPropertyValues(production(key, null)).run(context -> {
            assertThat(context).hasFailed();
            assertThat(NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure())).hasMessage(
                    "Perfil prod exige variaveis nao vazias: " + key);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"DB_URL", "DB_USERNAME", "DB_PASSWORD"})
    void productionRejectsBlankSettings(String key) {
        runner.withPropertyValues(production(key, "   ")).run(context -> {
            assertThat(context).hasFailed();
            assertThat(NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure())).hasMessage(
                    "Perfil prod exige variaveis nao vazias: " + key);
        });
    }

    @Test
    void developmentCannotOverrideProductionWithCombinedProfiles() {
        runner.withPropertyValues(production(null, null))
                .withPropertyValues("spring.profiles.active=prod,dev")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(NestedExceptionUtils.getMostSpecificCause(context.getStartupFailure())).hasMessage("Nao combine os perfis dev e prod");
                });
    }

    @Test
    void hostingPortIsOptionalAndUsesProviderValueWhenPresent() {
        runner.withPropertyValues(production(null, null)).run(context ->
                assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8080"));
        runner.withPropertyValues(production(null, null)).withPropertyValues("PORT=10000").run(context -> {
            assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("10000");
            assertThat(context.getEnvironment().getProperty("server.address")).isEqualTo("0.0.0.0");
            assertThat(context.getEnvironment().getProperty("server.shutdown")).isEqualTo("graceful");
        });
    }

    private String[] production(String changed, String value) {
        List<String> properties = new ArrayList<>();
        properties.add("spring.profiles.active=prod");
        for (String key : List.of("DB_URL", "DB_USERNAME", "DB_PASSWORD")) {
            if (key.equals(changed)) {
                if (value != null) properties.add(key + "=" + value);
            } else {
                properties.add(key + "=" + switch (key) {
                    case "DB_URL" -> "jdbc:postgresql://db:5432/demo";
                    case "DB_USERNAME" -> "demo";
                    default -> "test-fixture";
                });
            }
        }
        return properties.toArray(String[]::new);
    }
}
