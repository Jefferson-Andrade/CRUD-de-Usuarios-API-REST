package com.example.crud.exception;

public class InvalidUserQueryException extends RuntimeException {
    public InvalidUserQueryException(String message) {
        super(message);
    }
}
