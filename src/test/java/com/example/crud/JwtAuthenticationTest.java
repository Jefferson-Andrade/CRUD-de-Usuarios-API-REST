package com.example.crud;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired ObjectMapper mapper;
    @Autowired JwtEncoder jwtEncoder;

    @BeforeEach
    void accounts() {
        jdbc.update("DELETE FROM auth_accounts");
        String hash = encoder.encode("test-only-password");
        jdbc.update("INSERT INTO auth_accounts VALUES ('admin', ?, 'ADMIN', true)", hash);
        jdbc.update("INSERT INTO auth_accounts VALUES ('reader', ?, 'READER', true)", hash);
        jdbc.update("INSERT INTO auth_accounts VALUES ('disabled', ?, 'ADMIN', false)", hash);
    }

    private String login(String username, String password) throws Exception {
        String json = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Credentials(username, password))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("accessToken").asText();
    }

    record Credentials(String username, String password) {}

    @Test
    void loginAndReadWithBearer() throws Exception {
        String token = login("reader", "test-only-password");
        mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsAndDisabledAccountCannotLogin() throws Exception {
        for (String username : new String[]{"unknown", "disabled"}) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(new Credentials(username, "test-only-password"))))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new Credentials("admin", "incorrect"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bearerIsRejectedAfterAccountDisabled() throws Exception {
        String token = login("reader", "test-only-password");
        jdbc.update("UPDATE auth_accounts SET enabled = false WHERE username = 'reader'");
        mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer("crud-api").subject("reader")
                .issuedAt(now.minus(2, ChronoUnit.HOURS))
                .expiresAt(now.minus(1, ChronoUnit.HOURS)).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void modifiedSignatureIsRejected() throws Exception {
        String token = login("reader", "test-only-password");
        String changed = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");
        mvc.perform(get("/users").header("Authorization", "Bearer " + changed))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readerBearerCannotWrite() throws Exception {
        String token = login("reader", "test-only-password");
        mvc.perform(post("/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isForbidden());
    }
}
