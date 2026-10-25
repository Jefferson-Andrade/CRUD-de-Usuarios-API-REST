package com.example.crud.service;

import com.example.crud.dto.UserRequestDTO;
import com.example.crud.dto.UserResponseDTO;
import com.example.crud.exception.UserNotFoundException;
import com.example.crud.model.User;
import com.example.crud.repository.UserRepository;
import org.springframework.stereotype.Service;

import com.example.crud.dto.PageResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser(UserRequestDTO dto) {
        User user = new User();
        user.setName(dto.getName());

        User savedUser = userRepository.save(user);

        return toResponseDTO(savedUser);
    }

    public PageResponseDTO<UserResponseDTO> getAllUsers(String name, Pageable pageable) {
        String normalizedName = name == null ? "" : name.strip();
        Page<User> users = normalizedName.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByNameContainingIgnoreCase(normalizedName, pageable);
        return PageResponseDTO.from(users.map(this::toResponseDTO));
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return toResponseDTO(user);
    }

    public UserResponseDTO updateUser(Long id, UserRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        user.setName(dto.getName());

        // Flush runs @PreUpdate before mapping the response, so updatedAt is current in the PUT body.
        User updatedUser = userRepository.saveAndFlush(user);

        return toResponseDTO(updatedUser);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        userRepository.delete(user);
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
