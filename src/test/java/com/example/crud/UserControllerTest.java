package com.example.crud;

import com.example.crud.dto.UserResponseDTO;
import com.example.crud.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import com.example.crud.dto.PageResponseDTO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.mockito.Mockito.verifyNoInteractions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldCreateUser() throws Exception {
        given(userService.createUser(any()))
                .willReturn(new UserResponseDTO(1L, "Jefferson Andrade"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jefferson Andrade\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jefferson Andrade"));
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetUserById() throws Exception {
        given(userService.getUserById(1L))
                .willReturn(new UserResponseDTO(1L, "Jefferson Andrade"));

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldListUsersWithDefaults() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by("id"));
        given(userService.getAllUsers(null, pageable))
                .willReturn(new PageResponseDTO<>(List.of(new UserResponseDTO(1L, "Jefferson Andrade")),
                        0, 10, 1, 1, true, true));
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Jefferson Andrade"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
        verify(userService).getAllUsers(null, pageable);
    }

    @ParameterizedTest
    @CsvSource({"name,asc", "name,desc", "id,desc"})
    void shouldApplyQueryAndStableSorting(String field, String direction) throws Exception {
        Sort sorting = Sort.by(Sort.Direction.fromString(direction), field);
        if (field.equals("name")) sorting = sorting.and(Sort.by(Sort.Direction.fromString(direction), "id"));
        var pageable = PageRequest.of(1, 2, sorting);
        given(userService.getAllUsers("  JeF  ", pageable))
                .willReturn(new PageResponseDTO<>(List.of(), 1, 2, 2, 1, false, true));
        mockMvc.perform(get("/users").param("page", "1").param("size", "2")
                        .param("sort", field + "," + direction).param("name", "  JeF  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(2));
        verify(userService).getAllUsers("  JeF  ", pageable);
    }

    @ParameterizedTest
    @CsvSource({"page,-1", "page,abc", "page,2147483648", "size,0", "size,-1", "size,101", "size,abc",
            "sort,id", "sort,'id,up'", "sort,'email,asc'", "sort,'name,asc,id,desc'", "sort,'id,ASC'", "sort,','"})
    void shouldRejectInvalidQuery(String parameter, String value) throws Exception {
        mockMvc.perform(get("/users").param(parameter, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
        verifyNoInteractions(userService);
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"page", "size", "sort"})
    void shouldRejectEmptyParameter(String parameter) throws Exception {
        mockMvc.perform(get("/users").param(parameter, ""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectOffsetBeyondJpaLimit() throws Exception {
        mockMvc.perform(get("/users").param("page", "2147483647").param("size", "100"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectRepeatedSort() throws Exception {
        mockMvc.perform(get("/users").param("sort", "id,asc", "name,desc"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldRejectOverlongFilter() throws Exception {
        mockMvc.perform(get("/users").param("name", "a".repeat(256)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void shouldAcceptFilterAndSizeAtUpperLimits() throws Exception {
        String name = "a".repeat(255);
        var pageable = PageRequest.of(0, 100, Sort.by("id"));
        given(userService.getAllUsers(name, pageable))
                .willReturn(new PageResponseDTO<>(List.of(), 0, 100, 0, 0, true, true));
        mockMvc.perform(get("/users").param("name", name).param("size", "100"))
                .andExpect(status().isOk());
        verify(userService).getAllUsers(name, pageable);
    }

    @Test
    void shouldNotConvertUnexpectedServiceFailuresToBadRequest() {
        given(userService.getAllUsers(any(), any())).willThrow(new IllegalStateException("unexpected"));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mockMvc.perform(get("/users")))
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldUpdateUser() throws Exception {
        given(userService.updateUser(eq(1L), any()))
                .willReturn(new UserResponseDTO(1L, "Jefferson Atualizado"));

        mockMvc.perform(put("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jefferson Atualizado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jefferson Atualizado"));
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }
}
