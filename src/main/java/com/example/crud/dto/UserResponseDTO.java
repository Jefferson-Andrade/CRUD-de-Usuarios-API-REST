package com.example.crud.dto;

import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserResponseDTO {

    private Long id;
    private String name;

    @Schema(description = "E-mail normalizado; nulo para cadastros anteriores à V4 ainda não atualizados",
            example = "jefferson@example.com", format = "email", maxLength = 254,
            nullable = true)
    private String email;

    public String getEmail() {
        return email;
    }

    @Schema(description = "Data de criação em UTC; em registros legados, referência da migração V3",
            type = "string", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant createdAt;

    @Schema(description = "Data da última alteração persistida em UTC",
            type = "string", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
    private Instant updatedAt;

    public UserResponseDTO() {
    }

    public UserResponseDTO(Long id, String name) {
        this(id, name, null, null);
    }

    public UserResponseDTO(Long id, String name, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UserResponseDTO(Long id, String name, String email, Instant createdAt, Instant updatedAt) {
        this(id, name, createdAt, updatedAt);
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
