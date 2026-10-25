package com.example.crud;

import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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
@WithMockUser(roles = "ADMIN")
class UserPostgresIT {

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Test
    @org.springframework.security.test.context.support.WithAnonymousUser
    void shouldAuthenticateMigratedAccountsAndEnforceRoles() throws Exception {
        var bootstrap = new com.example.crud.config.AdminBootstrap(jdbc, passwordEncoder,
                "postgres-admin", "integration-test-password");
        bootstrap.run(null);
        String hash = jdbc.queryForObject("SELECT password_hash FROM auth_accounts WHERE username = 'postgres-admin'", String.class);
        assertTrue(passwordEncoder.matches("integration-test-password", hash));
        jdbc.update("INSERT INTO auth_accounts VALUES ('postgres-reader', ?, 'READER', true)", hash);
        try {
            mockMvc.perform(get("/users").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                            .httpBasic("postgres-reader", "integration-test-password")))
                    .andExpect(status().isOk());
            mockMvc.perform(post("/users").with(csrf())
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                    .httpBasic("postgres-reader", "integration-test-password"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/users").with(csrf())
                            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                    .httpBasic("postgres-admin", "integration-test-password"))
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                    .andExpect(status().isCreated());
            assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
            assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version='2' AND success=true", Long.class));
        } finally {
            jdbc.update("DELETE FROM auth_accounts WHERE username IN ('postgres-reader', 'postgres-admin')");
        }
    }

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
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(id))
                .andExpect(jsonPath("$.content[0].name").value("Jefferson Andrade"));
    }

    @Test
    void shouldPersistUpdatedName() throws Exception {
        long id = createUser();

        mockMvc.perform(put("/users/{id}", id).with(csrf())
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

        mockMvc.perform(delete("/users/{id}", id).with(csrf()))
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
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
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
        assertEquals("2", nextStartup.info().current().getVersion().getVersion());
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history", Long.class));
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
            assertEquals(1, legacy.migrate().migrationsExecuted);
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

    @Test
    void shouldPaginateInDatabaseWithoutOverlapAndKeepTotalsBeyondEnd() throws Exception {
        long ana = insertUser("Ana");
        long bruno = insertUser("Bruno");
        long carla = insertUser("Carla");
        mockMvc.perform(get("/users").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(ana))
                .andExpect(jsonPath("$.content[1].id").value(bruno))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
        mockMvc.perform(get("/users").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(carla))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true));
        mockMvc.perform(get("/users").param("page", "2").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldSearchPartialNameIgnoringCaseAndTrimmingEdges() throws Exception {
        long first = insertUser("Jefferson");
        insertUser("Ana");
        long second = insertUser("Maria JEFFERSON Andrade");
        mockMvc.perform(get("/users").param("name", "  jEfF  ").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(first))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        mockMvc.perform(get("/users").param("name", "  jEfF  ").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(second))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void shouldUseIdToBreakNameTiesInBothDirections() throws Exception {
        long firstAna = insertUser("Ana");
        long bruno = insertUser("Bruno");
        long secondAna = insertUser("Ana");
        mockMvc.perform(get("/users").param("sort", "name,asc").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(firstAna));
        mockMvc.perform(get("/users").param("sort", "name,asc").param("size", "1").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(secondAna));
        mockMvc.perform(get("/users").param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(bruno))
                .andExpect(jsonPath("$.content[1].id").value(secondAna))
                .andExpect(jsonPath("$.content[2].id").value(firstAna));
        mockMvc.perform(get("/users").param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(secondAna))
                .andExpect(jsonPath("$.content[1].id").value(bruno))
                .andExpect(jsonPath("$.content[2].id").value(firstAna));
    }

    @Test
    void shouldTreatEmptyAndWhitespaceFilterAsUnfiltered() throws Exception {
        insertUser("Ana");
        insertUser("Bruno");
        for (String name : new String[]{"", "   "}) {
            mockMvc.perform(get("/users").param("name", name))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    @Test
    void shouldReturnEmptyPageForUnmatchedFilterAndEmptyDatabase() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
        insertUser("Ana");
        mockMvc.perform(get("/users").param("name", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void shouldEscapeLikeWildcardsInNameFilter() throws Exception {
        long percent = insertUser("Ana 100%");
        long underscore = insertUser("Ana_Clara");
        insertUser("Ana Clara");
        mockMvc.perform(get("/users").param("name", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(percent))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/users").param("name", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(underscore))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void shouldDocumentPagedContractAndQueryLimitsInOpenApi() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var document = objectMapper.readTree(json);
        var operation = document.get("paths").get("/users").get("get");
        var schema = operation.get("responses").get("200").get("content")
                .get("application/json").get("schema");
        String reference = schema.get("$ref").asText();
        var properties = document.get("components").get("schemas")
                .get(reference.substring(reference.lastIndexOf('/') + 1)).get("properties");
        for (String property : new String[]{"content", "page", "size", "totalElements", "totalPages", "first", "last"}) {
            assertTrue(properties.has(property), property);
        }
        int documentedParameters = 0;
        for (var parameter : operation.get("parameters")) {
            String name = parameter.get("name").asText();
            var definition = parameter.get("schema");
            switch (name) {
                case "page" -> { assertEquals(0, definition.get("minimum").asInt()); documentedParameters++; }
                case "size" -> { assertEquals(100, definition.get("maximum").asInt()); documentedParameters++; }
                case "sort" -> { assertEquals("id,asc", definition.get("default").asText()); documentedParameters++; }
                case "name" -> { assertEquals(255, definition.get("maxLength").asInt()); documentedParameters++; }
                default -> fail("Unexpected API parameter: " + name);
            }
        }
        assertEquals(4, documentedParameters);
        assertTrue(operation.get("responses").has("400"));
    }

    @Test
    void shouldPersistNameAtLimitForCreateAndUpdate() throws Exception {
        String name = "a".repeat(255);
        String response = mockMvc.perform(post("/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("name", name))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(response).get("id").asLong();
        assertEquals(name, storedName(id));
        String updatedName = "b".repeat(255);
        mockMvc.perform(put("/users/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("name", updatedName))))
                .andExpect(status().isOk());
        assertEquals(updatedName, storedName(id));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
    }

    @Test
    void shouldPreserveDatabaseWhenWriteBodyIsInvalid() throws Exception {
        long id = createUser();
        String[] bodies = {"{}", "{\"name\":null}", "{\"name\":\"\"}", "{\"name\":\"   \"}",
                "{\"name\":\"" + "a".repeat(256) + "\"}", "{", "", "null", "[]", "{\"name\":{}}"};
        for (String body : bodies) {
            for (String method : new String[]{"POST", "PUT"}) {
                String path = method.equals("POST") ? "/users" : "/users/" + id;
                mockMvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path).with(csrf())
                                .contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400));
                assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
                assertEquals("Jefferson Andrade", storedName(id));
            }
        }
    }

    @Test
    void shouldReturnTypedErrorsForMissingUsers() throws Exception {
        for (String method : new String[]{"GET", "PUT", "DELETE"}) {
            mockMvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), "/users/9223372036854775807").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                    .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.timestamp").isString());
        }
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
    }

    private long insertUser(String name) {
        return jdbc.queryForObject("INSERT INTO users (name) VALUES (?) RETURNING id", Long.class, name);
    }

    private long createUser() throws Exception {
        String response = mockMvc.perform(post("/users").with(csrf())
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
