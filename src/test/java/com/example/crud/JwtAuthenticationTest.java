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

@SpringBootTest(properties = "app.security.login.max-attempts=100")
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
        jdbc.update("DELETE FROM users");
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
                .andExpect(header().string("Cache-Control", "no-store"))
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
        int signature = token.lastIndexOf('.') + 1;
        String changed = token.substring(0, signature)
                + (token.charAt(signature) == 'A' ? 'B' : 'A') + token.substring(signature + 1);
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

    @Test
    void adminBearerRequiresRealCsrfCookieThenCanCompleteCrud() throws Exception {
        String token = login("admin", "test-only-password");
        String body = "{\"name\":\"Ana\",\"email\":\"ana@example.com\"}";
        mvc.perform(post("/users").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Token CSRF ausente ou inválido"));
        var csrf = mvc.perform(get("/csrf").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrf);
        String created = mvc.perform(post("/users").header("Authorization", "Bearer " + token)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();
        mvc.perform(put("/users/" + id).header("Authorization", "Bearer " + token)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana Atualizada\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana Atualizada"));
        mvc.perform(delete("/users/" + id).header("Authorization", "Bearer " + token)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/users/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void readerCannotWriteEvenWithRealCsrfCookie() throws Exception {
        String token = login("reader", "test-only-password");
        var csrf = mvc.perform(get("/csrf").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(csrf);
        for (String method : new String[]{"POST", "PUT", "DELETE"}) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),
                    method.equals("POST") ? "/users" : "/users/1")
                    .header("Authorization", "Bearer " + token).cookie(csrf)
                    .header("X-XSRF-TOKEN", csrf.getValue()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\"}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Permissão insuficiente"));
        }
    }

    @Test
    void deletedAccountAndChangedRoleTakeEffectOnExistingToken() throws Exception {
        String token = login("reader", "test-only-password");
        jdbc.update("UPDATE auth_accounts SET role = 'ADMIN' WHERE username = 'reader'");
        var csrf = mvc.perform(get("/csrf").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        mvc.perform(post("/users").header("Authorization", "Bearer " + token)
                .cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isCreated());
        jdbc.update("DELETE FROM auth_accounts WHERE username = 'reader'");
        mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerAndMissingSubjectAreRejected() throws Exception {
        Instant now = Instant.now();
        var wrongIssuer = JwtClaimsSet.builder().issuer("another-api").subject("reader")
                .issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        var missingSubject = JwtClaimsSet.builder().issuer("crud-api")
                .issuedAt(now).expiresAt(now.plusSeconds(900)).build();
        for (var claims : new JwtClaimsSet[]{wrongIssuer, missingSubject}) {
            String token = jwtEncoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
            mvc.perform(get("/users").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void malformedAndOversizedLoginReturn400WithoutTokens() throws Exception {
        for (String body : new String[]{"{", "{}", "{\"username\":\" \",\"password\":\"x\"}",
                mapper.writeValueAsString(new Credentials("x".repeat(101), "x")),
                mapper.writeValueAsString(new Credentials("reader", "x".repeat(73))),
                mapper.writeValueAsString(new Credentials("reader", "é".repeat(37)))}) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.accessToken").doesNotExist());
        }
    }

    @Test
    void openApiOffersBasicOrBearerForCrudAndCsrf() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var paths = mapper.readTree(json).get("paths");
        for (var operation : new tools.jackson.databind.JsonNode[]{
                paths.get("/users").get("get"), paths.get("/csrf").get("get")}) {
            boolean basic = false;
            boolean bearer = false;
            for (var requirement : operation.get("security")) {
                basic |= requirement.has("basicAuth") && !requirement.has("bearerAuth");
                bearer |= requirement.has("bearerAuth") && !requirement.has("basicAuth");
            }
            assertTrue(basic && bearer, "Basic e Bearer devem ser alternativas");
        }
    }
}
