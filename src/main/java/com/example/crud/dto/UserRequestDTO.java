package com.example.crud.dto;

import jakarta.validation.constraints.NotBlank;

public class UserRequestDTO {

    @NotBlank(message = "O nome é obrigatório")
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