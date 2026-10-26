package com.example.crud;

import com.example.crud.config.DemoReaderBootstrap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DemoReaderBootstrapTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired MockMvc mvc;
    private static final String PASSWORD = "reader-test-password";

    @BeforeEach
    void cleanAccounts() {
        jdbc.update("DELETE FROM auth_accounts WHERE username = 'demo-reader'");
    }

    @Test
    void provisionsHashedReaderThatAuthenticatesButCannotWriteOrInspectHealth() throws Exception {
        bootstrap(PASSWORD).run(null);
        var account = jdbc.queryForMap("SELECT * FROM auth_accounts WHERE username = 'demo-reader'");
        assertEquals("READER", account.get("role"));
        assertEquals(true, account.get("enabled"));
        assertNotEquals(PASSWORD, account.get("password_hash"));
        assertTrue(encoder.matches(PASSWORD, (String) account.get("password_hash")));
        mvc.perform(get("/users").with(httpBasic("demo-reader", PASSWORD))).andExpect(status().isOk());
        mvc.perform(get("/actuator/health").with(httpBasic("demo-reader", PASSWORD)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health/db").with(httpBasic("demo-reader", PASSWORD)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/users").with(httpBasic("demo-reader", PASSWORD)).with(csrf())
                        .contentType("application/json").content("{\"name\":\"Demo\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedStartsPreservePasswordAndRole() {
        bootstrap(PASSWORD).run(null);
        String original = jdbc.queryForObject("SELECT password_hash FROM auth_accounts WHERE username = 'demo-reader'", String.class);
        bootstrap("another-test-password").run(null);
        assertEquals(original, jdbc.queryForObject("SELECT password_hash FROM auth_accounts WHERE username = 'demo-reader'", String.class));
    }

    @Test
    void refusesExistingAdminWithoutChangingIt() {
        jdbc.update("INSERT INTO auth_accounts VALUES ('demo-reader', ?, 'ADMIN', true)", encoder.encode(PASSWORD));
        assertThrows(IllegalStateException.class, () -> bootstrap(PASSWORD).run(null));
        assertEquals("ADMIN", jdbc.queryForObject("SELECT role FROM auth_accounts WHERE username = 'demo-reader'", String.class));
    }

    @Test
    void refusesDisabledReaderWithoutReenablingIt() {
        jdbc.update("INSERT INTO auth_accounts VALUES ('demo-reader', ?, 'READER', false)", encoder.encode(PASSWORD));
        assertThrows(IllegalStateException.class, () -> bootstrap(PASSWORD).run(null));
        assertEquals(false, jdbc.queryForObject("SELECT enabled FROM auth_accounts WHERE username = 'demo-reader'", Boolean.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "short", "                                                                        ", "ééééééééééééééééééééééééééééééééééééé"})
    void rejectsInvalidPasswordsBeforeInsert(String password) {
        assertThrows(IllegalStateException.class, () -> bootstrap(password).run(null));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM auth_accounts WHERE username = 'demo-reader'", Integer.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "invalid username", "invalid@username"})
    void rejectsInvalidUsernames(String username) {
        assertThrows(IllegalStateException.class,
                () -> new DemoReaderBootstrap(jdbc, encoder, username, PASSWORD, "").run(null));
    }

    @Test
    void refusesSameConfiguredAdminNameBeforeEitherAccountExists() {
        assertThrows(IllegalStateException.class,
                () -> new DemoReaderBootstrap(jdbc, encoder, "demo-reader", PASSWORD, "demo-reader").run(null));
    }

    private DemoReaderBootstrap bootstrap(String password) {
        return new DemoReaderBootstrap(jdbc, encoder, "demo-reader", password, "");
    }
}
