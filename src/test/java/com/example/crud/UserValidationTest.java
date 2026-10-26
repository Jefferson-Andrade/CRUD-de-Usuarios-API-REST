package com.example.crud;

import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.example.crud.dto.UserResponseDTO;
import com.example.crud.exception.UserNotFoundException;
import com.example.crud.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.context.annotation.Import(com.example.crud.config.SecurityConfig.class)
@WebMvcTest
@WithMockUser(roles = "ADMIN")
class UserValidationTest {
    @MockitoBean org.springframework.security.core.userdetails.UserDetailsService accounts;
    @MockitoBean org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtEncoder jwtEncoder;
    @MockitoBean org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean UserService service;

    static Stream<Arguments> invalidNames() {
        return Stream.of("POST", "PUT").flatMap(method -> Stream.of(
                Arguments.of(method, "{\"email\":\"fixture@example.com\"}", "O nome é obrigatório"),
                Arguments.of(method, "{\"email\":\"fixture@example.com\",\"name\":null}", "O nome é obrigatório"),
                Arguments.of(method, "{\"email\":\"fixture@example.com\",\"name\":\"\"}", "O nome é obrigatório"),
                Arguments.of(method, "{\"email\":\"fixture@example.com\",\"name\":\"   \"}", "O nome é obrigatório"),
                Arguments.of(method, "{\"email\":\"fixture@example.com\",\"name\":\"" + "a".repeat(256) + "\"}",
                        "O nome deve ter no máximo 255 caracteres"),
                Arguments.of(method, "{\"email\":\"fixture@example.com\",\"name\":\"" + " ".repeat(256) + "\"}", "O nome é obrigatório")));
    }

    @ParameterizedTest @MethodSource("invalidNames")
    void rejectsInvalidNamesBeforeCallingService(String method, String body, String message) throws Exception {
        expectError(mvc.perform(request(HttpMethod.valueOf(method), path(method)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body)), 400, "Bad Request", message);
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PUT"})
    void acceptsMaximumNameWithoutNormalizing(String method) throws Exception {
        String name = " " + "a".repeat(253) + " ";
        given(service.createUser(any())).willReturn(new UserResponseDTO(1L, name));
        given(service.updateUser(eq(1L), any())).willReturn(new UserResponseDTO(1L, name));
        mvc.perform(request(HttpMethod.valueOf(method), path(method)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(java.util.Map.of("name", name, "email", "fixture@example.com"))))
                .andExpect(status().is(method.equals("POST") ? 201 : 200))
                .andExpect(jsonPath("$.name").value(name));
        if (method.equals("POST")) verify(service).createUser(argThat(dto -> name.equals(dto.getName())));
        else verify(service).updateUser(eq(1L), argThat(dto -> name.equals(dto.getName())));
    }

    static Stream<Arguments> invalidBodies() {
        return Stream.of("POST", "PUT").flatMap(method -> Stream.of("", "{", "null", "[]", "{\"email\":\"fixture@example.com\",\"name\":{}}")
                .map(body -> Arguments.of(method, body)));
    }

    @ParameterizedTest @MethodSource("invalidBodies")
    void rejectsUnreadableBodiesWithoutExposingParserDetails(String method, String body) throws Exception {
        expectError(mvc.perform(request(HttpMethod.valueOf(method), path(method)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body)),
                400, "Bad Request", "Corpo da requisição ausente ou JSON inválido");
        verifyNoInteractions(service);
    }

    @ParameterizedTest @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void preservesNotFoundContract(String method) throws Exception {
        given(service.getUserById(999L)).willThrow(new UserNotFoundException(999L));
        given(service.updateUser(eq(999L), any())).willThrow(new UserNotFoundException(999L));
        doThrow(new UserNotFoundException(999L)).when(service).deleteUser(999L);
        expectError(mvc.perform(request(HttpMethod.valueOf(method), "/users/999").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"fixture@example.com\",\"name\":\"Ana\"}")),
                404, "Not Found", "Usuário não encontrado: 999");
    }

    @ParameterizedTest @ValueSource(strings = {"POST", "PUT"})
    void doesNotMaskUnexpectedWriteFailures(String method) {
        given(service.createUser(any())).willThrow(new IllegalStateException("internal failure"));
        given(service.updateUser(eq(1L), any())).willThrow(new IllegalStateException("internal failure"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mvc.perform(
                        request(HttpMethod.valueOf(method), path(method)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"fixture@example.com\",\"name\":\"Ana\"}")))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    private String path(String method) { return method.equals("POST") ? "/users" : "/users/1"; }

    private void expectError(ResultActions result, int status, String error, String message) throws Exception {
        result.andExpect(status().is(status)).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(status)).andExpect(jsonPath("$.error").value(error))
                .andExpect(jsonPath("$.message").value(message)).andExpect(response -> {
                    var json = mapper.readTree(response.getResponse().getContentAsString());
                    assertEquals(4, json.size());
                    assertTrue(json.get("timestamp").isString());
                    assertDoesNotThrow(() -> LocalDateTime.parse(json.get("timestamp").asText()));
                });
    }
}
