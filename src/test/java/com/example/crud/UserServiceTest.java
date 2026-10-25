package com.example.crud;

import com.example.crud.dto.UserRequestDTO;
import com.example.crud.dto.UserResponseDTO;
import com.example.crud.dto.PageResponseDTO;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldListUsersWithoutFilter(String name) {
        User user = new User();
        user.setId(1L);
        user.setName("Jefferson Andrade");
        var pageable = PageRequest.of(0, 2, Sort.by("id"));
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 5));

        PageResponseDTO<UserResponseDTO> response = userService.getAllUsers(name, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().getId()).isEqualTo(1L);
        assertThat(response.content().getFirst().getName()).isEqualTo("Jefferson Andrade");
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isFalse();
        verify(userRepository).findAll(pageable);
        verify(userRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void shouldNormalizeFilterAndKeepFilteredTotals() {
        User user = new User();
        user.setId(3L);
        user.setName("Jefferson");
        var pageable = PageRequest.of(1, 1, Sort.by("name").and(Sort.by("id")));
        when(userRepository.findByNameContainingIgnoreCase("JeF", pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 2));
        var response = userService.getAllUsers("  JeF  ", pageable);
        assertThat(response.content().getFirst().getId()).isEqualTo(3L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.first()).isFalse();
        assertThat(response.last()).isTrue();
        verify(userRepository, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void shouldUpdateUser() {
        User user = new User();
        user.setId(1L);
        user.setName("Jefferson");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

        UserResponseDTO response = userService.updateUser(
                1L, new UserRequestDTO("Jefferson Atualizado"));

        assertThat(response.getName()).isEqualTo("Jefferson Atualizado");
        verify(userRepository).saveAndFlush(user);
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
