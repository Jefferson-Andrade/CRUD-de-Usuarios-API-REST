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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
class UserControllerIntegrationTest {

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
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jefferson Andrade"));
    }

    @Test
    void shouldListUsers() throws Exception {
        given(userService.getAllUsers())
                .willReturn(List.of(new UserResponseDTO(1L, "Jefferson Andrade")));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Jefferson Andrade"));
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
