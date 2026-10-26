package com.example.crud;

import com.example.crud.config.DemoDataBootstrap;
import com.example.crud.config.DemoReaderBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DemoOptInTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class))
            .withUserConfiguration(DemoReaderBootstrap.class, DemoDataBootstrap.class);

    @Test
    void demoIsAbsentByDefaultEvenIfSeedFlagIsTrue() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(DemoReaderBootstrap.class);
            assertThat(context).doesNotHaveBean(DemoDataBootstrap.class);
        });
        runner.withPropertyValues("app.demo.seed-data=true").run(context ->
                assertThat(context).doesNotHaveBean(DemoDataBootstrap.class));
    }

    @Test
    void enablingReaderDoesNotEnableFixtures() {
        runner.withPropertyValues("app.demo.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(DemoReaderBootstrap.class);
            assertThat(context).doesNotHaveBean(DemoDataBootstrap.class);
        });
    }

    @Test
    void fixturesRequireBothExplicitFlags() {
        runner.withPropertyValues("app.demo.enabled=true", "app.demo.seed-data=true").run(context -> {
            assertThat(context).hasSingleBean(DemoReaderBootstrap.class);
            assertThat(context).hasSingleBean(DemoDataBootstrap.class);
        });
    }
}
