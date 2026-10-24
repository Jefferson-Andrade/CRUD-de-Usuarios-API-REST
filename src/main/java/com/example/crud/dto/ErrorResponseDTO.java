package com.example.crud.dto;

import java.time.LocalDateTime;

public record ErrorResponseDTO(int status, String error, String message, LocalDateTime timestamp) {
}
