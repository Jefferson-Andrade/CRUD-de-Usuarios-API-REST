package com.example.crud;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class UserAuditTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    private Cookie csrfCookie;
    private static final Instant OLD = Instant.parse("2020-01-02T03:04:05Z");

    @BeforeEach
    void setup() throws Exception {
        jdbc.update("DELETE FROM users");
        csrfCookie = mvc.perform(get("/csrf")).andExpect(status().isOk())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrfCookie);
    }

    @Test
    void creationIsAutomaticAndDatesAreVisibleInGetAndList() throws Exception {
        Instant before = Instant.now().minusSeconds(1);
        JsonNode created = create();
        Instant at = Instant.parse(created.get("createdAt").asText());
        assertFalse(at.isBefore(before));
        assertFalse(at.isAfter(Instant.now()));
        assertEquals(created.get("createdAt"), created.get("updatedAt"));
        long id = created.get("id").asLong();
        assertEquals(at, stored("created_at", id));
        JsonNode found = read(get("/users/{id}", id), 200);
        JsonNode listed = read(get("/users"), 200).get("content").get(0);
        assertEquals(created.get("createdAt"), found.get("createdAt"));
        assertEquals(created.get("updatedAt"), listed.get("updatedAt"));
    }

    @Test
    void updatePreservesCreationAndReturnsTheNewPersistedUpdateTime() throws Exception {
        long id = create().get("id").asLong();
        seedOldDates(id);
        JsonNode updated = read(write(put("/users/{id}", id))
                .content("{\"email\":\"fixture@example.com\",\"name\":\"Nome atualizado\"}"), 200);
        Instant changed = Instant.parse(updated.get("updatedAt").asText());
        assertTrue(changed.isAfter(OLD));
        assertEquals(OLD, Instant.parse(updated.get("createdAt").asText()));
        assertEquals(OLD, stored("created_at", id));
        assertEquals(changed, stored("updated_at", id));
    }

    @Test
    void inputDatesCannotOverrideServerDatesOnCreateOrUpdate() throws Exception {
        String spoof = ",\"createdAt\":\"2000-01-01T00:00:00Z\",\"updatedAt\":\"2099-01-01T00:00:00Z\"}";
        JsonNode created = read(write(post("/users")).content("{\"email\":\"fixture@example.com\",\"name\":\"Ana\"" + spoof), 201);
        long id = created.get("id").asLong();
        assertNotEquals("2000-01-01T00:00:00Z", created.get("createdAt").asText());
        assertEquals(created.get("createdAt"), created.get("updatedAt"));
        seedOldDates(id);
        JsonNode updated = read(write(put("/users/{id}", id)).content("{\"email\":\"fixture@example.com\",\"name\":\"Maria\"" + spoof), 200);
        assertEquals(OLD, Instant.parse(updated.get("createdAt").asText()));
        assertTrue(Instant.parse(updated.get("updatedAt").asText()).isBefore(Instant.parse("2099-01-01T00:00:00Z")));
    }

    @Test
    void unchangedNameDoesNotChangeAuditDates() throws Exception {
        long id = create().get("id").asLong();
        seedOldDates(id);
        JsonNode unchanged = read(write(put("/users/{id}", id)).content("{\"email\":\"fixture@example.com\",\"name\":\"Ana\"}"), 200);
        assertEquals(OLD, Instant.parse(unchanged.get("createdAt").asText()));
        assertEquals(OLD, Instant.parse(unchanged.get("updatedAt").asText()));
        assertEquals(OLD, stored("updated_at", id));
    }

    @Test
    void validationFailureDoesNotChangeAuditDates() throws Exception {
        long id = create().get("id").asLong();
        seedOldDates(id);
        read(write(put("/users/{id}", id)).content("{\"email\":\"fixture@example.com\",\"name\":\"\"}"), 400);
        assertEquals(OLD, stored("created_at", id));
        assertEquals(OLD, stored("updated_at", id));
        assertEquals("Ana", jdbc.queryForObject("SELECT name FROM users WHERE id = ?", String.class, id));
    }

    private JsonNode create() throws Exception {
        return read(write(post("/users")).content("{\"email\":\"fixture@example.com\",\"name\":\"Ana\"}"), 201);
    }

    private MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request) {
        return request.cookie(csrfCookie).header("X-XSRF-TOKEN", csrfCookie.getValue())
                .contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode read(MockHttpServletRequestBuilder request, int status) throws Exception {
        return mapper.readTree(mvc.perform(request).andExpect(status().is(status))
                .andReturn().getResponse().getContentAsString());
    }

    private void seedOldDates(long id) {
        jdbc.update("UPDATE users SET created_at = ?, updated_at = ? WHERE id = ?",
                OffsetDateTime.parse(OLD.toString()), OffsetDateTime.parse(OLD.toString()), id);
    }

    private Instant stored(String column, long id) {
        return jdbc.queryForObject("SELECT " + column + " FROM users WHERE id = ?", OffsetDateTime.class, id).toInstant();
    }
}
