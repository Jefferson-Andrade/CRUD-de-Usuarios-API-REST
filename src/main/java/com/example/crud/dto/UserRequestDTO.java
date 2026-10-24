package com.example.crud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
    @Schema(description = "Nome obrigatório; limite de 255 unidades UTF-16 (String.length), sem normalização",
            example = "Jefferson Andrade", maxLength = 255)
    private String name;

    public UserRequestDTO() {
    }

    public UserRequestDTO(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
