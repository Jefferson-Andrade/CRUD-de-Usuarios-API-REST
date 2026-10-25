package com.example.crud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email;
import java.util.Locale;
import io.swagger.v3.oas.annotations.media.Schema;

public class UserRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
    @Schema(description = "Nome obrigatório; limite de 255 unidades UTF-16 (String.length), sem normalização",
            example = "Jefferson Andrade", maxLength = 255)
    private String name;

    @NotBlank(message = "O e-mail é obrigatório")
    @Email(message = "O e-mail deve ser válido")
    @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres")
    @Schema(description = "E-mail obrigatório e único, sem diferenciar caixa; espaços nas pontas são removidos",
            example = "jefferson@example.com", format = "email", maxLength = 254,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public UserRequestDTO() {
    }

    public UserRequestDTO(String name) {
        this.name = name;
    }

    public UserRequestDTO(String name, String email) {
        this.name = name;
        setEmail(email);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
