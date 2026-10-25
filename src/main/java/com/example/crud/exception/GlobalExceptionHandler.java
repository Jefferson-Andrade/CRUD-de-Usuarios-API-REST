package com.example.crud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import com.example.crud.dto.ErrorResponseDTO;
import java.util.Comparator;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotFound(UserNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(org.springframework.validation.FieldError::getField)
                        .thenComparing(error -> String.valueOf(error.getCode()))
                        .thenComparing(error -> String.valueOf(error.getDefaultMessage())))
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Dados inválidos");

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(InvalidUserQueryException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidQuery(InvalidUserQueryException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Parâmetro '" + ex.getName() + "' inválido: informe um número inteiro válido");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou JSON inválido");
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleIntegrity(org.springframework.dao.DataIntegrityViolationException ex) {
        // Match only the email index. Other integrity failures keep their original error semantics.
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && "uk_users_email".equalsIgnoreCase(violation.getConstraintName())) {
                return buildResponse(HttpStatus.CONFLICT, "O e-mail já está cadastrado");
            }
        }
        throw ex;
    }

    private ResponseEntity<ErrorResponseDTO> buildResponse(HttpStatus status, String message) {
        ErrorResponseDTO body = new ErrorResponseDTO(status.value(), status.getReasonPhrase(), message, LocalDateTime.now());

        return ResponseEntity.status(status).body(body);
    }
}
