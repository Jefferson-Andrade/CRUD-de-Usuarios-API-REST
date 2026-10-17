package com.example.crud;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@Import(UserPostgresIT.PostgresConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserPostgresIT {

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgresConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("crud_integration")
                    .withUsername("crud_test")
                    .withPassword("crud_test");
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clearDatabase() {
        jdbc.update("DELETE FROM users");
    }

    @Test
    void shouldCreatePersistReadAndListUser() throws Exception {
        long id = createUser();

        assertTrue(id > 0);
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        assertEquals("Jefferson Andrade", storedName(id));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Jefferson Andrade"));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].name").value("Jefferson Andrade"));
    }

    @Test
    void shouldPersistUpdatedName() throws Exception {
        long id = createUser();

        mockMvc.perform(put("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jefferson Atualizado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Jefferson Atualizado"));

        assertEquals("Jefferson Atualizado", storedName(id));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jefferson Atualizado"));
    }

    @Test
    void shouldDeletePersistedUserAndReturnNotFound() throws Exception {
        long id = createUser();

        mockMvc.perform(delete("/users/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertEquals(0L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?", Long.class, id));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldRecordV1AndPreserveDataWhenMigrationsRunAgain() throws Exception {
        assertEquals(1L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' "
                        + "AND script = 'V1__create_users_table.sql' AND success = true", Long.class));
        long id = createUser();

        // Uma nova instância simula a execução das migrações na próxima inicialização.
        Flyway nextStartup = Flyway.configure()
                .dataSource(jdbc.getDataSource())
                .locations("classpath:db/migration")
                .baselineOnMigrate(false)
                .load();
        assertEquals(0, nextStartup.migrate().migrationsExecuted);
        assertEquals("1", nextStartup.info().current().getVersion().getVersion());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Long.class));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        assertEquals("Jefferson Andrade", storedName(id));
    }

    @Test
    void shouldAdoptExistingSchemaWithExplicitBaselineAndPreserveUsers() {
        // Schema isolado no PostgreSQL temporário; nunca usa o banco local da aplicação.
        jdbc.execute("CREATE SCHEMA legacy_adoption");
        try {
            jdbc.execute("CREATE TABLE legacy_adoption.users ("
                    + "id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, name VARCHAR(255))");
            Long originalId = jdbc.queryForObject(
                    "INSERT INTO legacy_adoption.users (name) VALUES ('Usuário existente') RETURNING id",
                    Long.class);
            Flyway legacy = Flyway.configure()
                    .dataSource(jdbc.getDataSource())
                    .defaultSchema("legacy_adoption")
                    .schemas("legacy_adoption")
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(false)
                    .baselineVersion("1")
                    .load();

            // A configuração padrão deve recusar adoção silenciosa de um schema não vazio.
            assertThrows(org.flywaydb.core.api.FlywayException.class, legacy::migrate);
            legacy.baseline();
            assertEquals(0, legacy.migrate().migrationsExecuted);
            assertEquals(1L, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM legacy_adoption.flyway_schema_history "
                            + "WHERE version = '1' AND type = 'BASELINE' AND success = true", Long.class));
            assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM legacy_adoption.users", Long.class));
            assertEquals("Usuário existente", jdbc.queryForObject(
                    "SELECT name FROM legacy_adoption.users WHERE id = ?", String.class, originalId));
            Long newId = jdbc.queryForObject(
                    "INSERT INTO legacy_adoption.users (name) VALUES ('Novo usuário') RETURNING id", Long.class);
            assertTrue(newId > originalId);
        } finally {
            jdbc.execute("DROP SCHEMA legacy_adoption CASCADE");
        }
    }

    private long createUser() throws Exception {
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jefferson Andrade\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Jefferson Andrade"))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private String storedName(long id) {
        return jdbc.queryForObject("SELECT name FROM users WHERE id = ?", String.class, id);
    }
}
