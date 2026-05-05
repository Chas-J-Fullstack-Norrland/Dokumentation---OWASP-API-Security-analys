package com.example.library.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

// Minimal request body for borrowing a book through the v1 API.
@Schema(
        name = "CreateLoanRequestV1",
        description = "Request body for creating a new loan in API version 1"
)
public record CreateLoanRequestV1(
        @Schema(
                description = "Id of the book to loan",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Book id is required")
        Long bookId
) {
}
