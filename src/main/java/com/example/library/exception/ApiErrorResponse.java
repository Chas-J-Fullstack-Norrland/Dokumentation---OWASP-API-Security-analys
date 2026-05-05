package com.example.library.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ApiErrorResponse", description = "Standardized error response")
public record ApiErrorResponse(
        @Schema(description = "Time when the error occurred", example = "2025-09-01T10:00:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "HTTP error reason", example = "Not Found")
        String error,

        @Schema(description = "Human-readable error message", example = "Book with id 99 not found")
        String message,

        @Schema(description = "Request path that caused the error", example = "/api/v1/books/99")
        String path
) {
}
