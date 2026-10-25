package com.example.crud;

import com.example.crud.config.AdminBootstrap;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    private static final String PASSWORD = "test-only-password";

    @BeforeEach
    void accounts() {
        jdbc.update("DELETE FROM auth_accounts");
        String hash = encoder.encode(PASSWORD);
        for (String role : new String[]{"ADMIN", "READER"}) {
            jdbc.update("INSERT INTO auth_accounts VALUES (?, ?, ?, true)", role.toLowerCase(), hash, role);
        }
        jdbc.update("INSERT INTO auth_accounts VALUES ('disabled', ?, 'ADMIN', false)", hash);
    }

    @Test
    void anonymousReadIs401WithBasicChallengeAndTypedError() throws Exception {
        mvc.perform(get("/users")).andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Basic realm=\"crud-api\""))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @ParameterizedTest @ValueSource(strings = {"missing", "disabled", "admin"})
    void wrongPasswordUnknownAndDisabledAreRejected(String name) throws Exception {
        mvc.perform(get("/users").with(httpBasic(name, name.equals("admin") ? "wrong" : PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticação necessária ou credenciais inválidas"));
    }

    @ParameterizedTest @ValueSource(strings = {"admin", "reader"})
    void bothRolesReadWithoutCreatingSession(String username) throws Exception {
        mvc.perform(get("/users").with(httpBasic(username, PASSWORD)))
                .andExpect(status().isOk()).andExpect(result -> assertNull(result.getRequest().getSession(false)));
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void readerCannotWriteEvenWithCsrf(String method) throws Exception {
        Cookie token = mvc.perform(get("/csrf").with(httpBasic("reader", PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(token);
        mvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? "/users" : "/users/1")
                        .with(httpBasic("reader", PASSWORD)).cookie(token).header("X-XSRF-TOKEN", token.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Permissão insuficiente"));
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PUT", "DELETE"})
    void adminNeedsCsrfForEveryWrite(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? "/users" : "/users/1")
                        .with(httpBasic("admin", PASSWORD)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value("Token CSRF ausente ou inválido"));
    }

    @Test
    void realCookieAndHeaderAllowFullAdminCrud() throws Exception {
        var response = mvc.perform(get("/csrf").with(httpBasic("admin", PASSWORD)))
                .andExpect(status().isOk()).andReturn().getResponse();
        Cookie cookie = response.getCookie("XSRF-TOKEN");
        assertNotNull(cookie);
        var token = mapper.readTree(response.getContentAsString());
        assertEquals(cookie.getValue(), token.get("token").asText());
        String header = token.get("headerName").asText();
        String json = mvc.perform(post("/users").with(httpBasic("admin", PASSWORD))
                        .cookie(cookie).header(header, cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(json).get("id").asLong();
        mvc.perform(put("/users/{id}", id).with(httpBasic("admin", PASSWORD))
                        .cookie(cookie).header(header, cookie.getValue())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Maria\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/users/{id}", id).with(httpBasic("admin", PASSWORD))
                        .cookie(cookie).header(header, cookie.getValue())).andExpect(status().isNoContent());
    }

    @Test
    void mismatchedCsrfIsRejected() throws Exception {
        mvc.perform(post("/users").with(httpBasic("admin", PASSWORD))
                        .cookie(new Cookie("XSRF-TOKEN", "one")).header("X-XSRF-TOKEN", "two")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void docsArePublicButUnknownRoutesRemainDenied() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/not-allowed").with(httpBasic("admin", PASSWORD))).andExpect(status().isForbidden());
    }

    @Test
    void bootstrapHashesAndNeverOverwritesExistingAccounts() {
        var bootstrap = new AdminBootstrap(jdbc, encoder, "initial-admin", PASSWORD);
        bootstrap.run(null);
        String hash = jdbc.queryForObject("SELECT password_hash FROM auth_accounts WHERE username = 'initial-admin'", String.class);
        assertNotEquals(PASSWORD, hash);
        assertTrue(encoder.matches(PASSWORD, hash));
        new AdminBootstrap(jdbc, encoder, "initial-admin", "different-password").run(null);
        assertEquals(hash, jdbc.queryForObject("SELECT password_hash FROM auth_accounts WHERE username = 'initial-admin'", String.class));
    }

    @Test
    void bootstrapRequiresCompleteStrongConfigurationAndDoesNothingWhenAbsent() {
        long count = jdbc.queryForObject("SELECT COUNT(*) FROM auth_accounts", Long.class);
        new AdminBootstrap(jdbc, encoder, "", "").run(null);
        assertEquals(count, jdbc.queryForObject("SELECT COUNT(*) FROM auth_accounts", Long.class));
        for (String password : new String[]{"", "short", "é".repeat(37)}) {
            assertThrows(IllegalStateException.class,
                    () -> new AdminBootstrap(jdbc, encoder, "new-admin", password).run(null));
        }
        assertThrows(IllegalStateException.class, () -> new AdminBootstrap(jdbc, encoder, "", PASSWORD).run(null));
    }
}
