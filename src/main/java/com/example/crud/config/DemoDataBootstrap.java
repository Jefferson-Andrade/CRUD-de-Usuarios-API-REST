package com.example.crud.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** PostgreSQL-only fixtures for an explicitly selected demonstration database. */
@Component
@ConditionalOnProperty(name = {"app.demo.enabled", "app.demo.seed-data"}, havingValue = "true")
public class DemoDataBootstrap implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public DemoDataBootstrap(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Repeated or concurrent starts preserve existing rows and their timestamps.
        for (String[] user : new String[][] {
                {"Ana Demonstracao", "ana.demo@example.com"},
                {"Bruno Demonstracao", "bruno.demo@example.com"},
                {"Carla Demonstracao", "carla.demo@example.com"}}) {
            jdbc.update("INSERT INTO users (name, email) VALUES (?, ?) ON CONFLICT DO NOTHING",
                    user[0], user[1]);
        }
    }
}
