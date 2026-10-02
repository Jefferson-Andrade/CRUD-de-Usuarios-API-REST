package com.example.crud;

import com.example.crud.dto.UserRequestDTO;
import com.example.crud.dto.UserResponseDTO;
import com.example.crud.exception.UserNotFoundException;
import com.example.crud.model.User;
import com.example.crud.repository.UserRepository;
import com.example.crud.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void shouldCreateUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Jefferson Andrade");
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDTO response = userService.createUser(new UserRequestDTO("Jefferson Andrade"));

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Jefferson Andrade");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldFindUserById() {
        User user = new User();
        user.setId(1L);
        user.setName("Jefferson Andrade");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponseDTO response = userService.getUserById(1L);

        assertThat(response.getName()).isEqualTo("Jefferson Andrade");
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldListUsers() {
        User user = new User();
        user.setId(1L);
        user.setName("Jefferson Andrade");
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponseDTO> response = userService.getAllUsers();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().getName()).isEqualTo("Jefferson Andrade");
    }

    @Test
    void shouldUpdateUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Jefferson");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UserResponseDTO response = userService.updateUser(
                1L, new UserRequestDTO("Jefferson Atualizado"));

        assertThat(response.getName()).isEqualTo("Jefferson Atualizado");
        verify(userRepository).save(user);
    }

    @Test
    void shouldDeleteUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Jefferson");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }
}
