package com.example.crud;

import com.example.crud.exception.GlobalExceptionHandler;
import com.example.crud.service.UserService;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(com.example.crud.config.SecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class UserEmailTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean UserService service;
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService accounts;

    static Stream<Arguments> invalidEmails() {
        return Stream.of("POST", "PUT").flatMap(method ->
                Stream.of(null, "", "   ", "invalid", "a@", "@example.com", "a b@example.com",
                                "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(62))
                        .map(email -> Arguments.of(method, email)));
    }

    @ParameterizedTest @MethodSource("invalidEmails")
    void rejectsInvalidEmailBeforeCallingService(String method, String email) throws Exception {
        var body = new HashMap<String, String>();
        body.put("name", "Ana");
        body.put("email", email);
        mvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? "/users" : "/users/1")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingEmail() throws Exception {
        for (String method : new String[]{"POST", "PUT"}) {
            mvc.perform(request(HttpMethod.valueOf(method), method.equals("POST") ? "/users" : "/users/1")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ana\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("O e-mail é obrigatório"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void acceptsMaximumEmailAndNormalizesBeforeService() throws Exception {
        String email = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertEquals(254, email.length());
        mvc.perform(post("/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Ana", "email", email))))
                .andExpect(status().isCreated());
        verify(service).createUser(argThat(dto -> email.equals(dto.getEmail())));
        mvc.perform(put("/users/1").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("name", "Ana", "email", "  Ana@Example.COM  "))))
                .andExpect(status().isOk());
        verify(service).updateUser(eq(1L), argThat(dto -> "ana@example.com".equals(dto.getEmail())));
    }

    @Test
    void mapsOnlyEmailConstraintToConflict() throws Exception {
        var violation = new ConstraintViolationException("duplicate", new SQLException("sensitive SQL"), "uk_users_email");
        when(service.createUser(any())).thenThrow(new DataIntegrityViolationException("internal details", violation));
        mvc.perform(post("/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("O e-mail já está cadastrado"))
                .andExpect(jsonPath("$.timestamp").isString());
        var other = new DataIntegrityViolationException("other constraint",
                new ConstraintViolationException("failure", new SQLException(), "other_constraint"));
        assertSame(other, assertThrows(DataIntegrityViolationException.class,
                () -> new GlobalExceptionHandler().handleIntegrity(other)));
        var unknown = new DataIntegrityViolationException("unknown");
        assertSame(unknown, assertThrows(DataIntegrityViolationException.class,
                () -> new GlobalExceptionHandler().handleIntegrity(unknown)));
    }
}
