package com.example.library.exception;

public class ExternalServiceUnavailableException extends RuntimeException {
    public ExternalServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}