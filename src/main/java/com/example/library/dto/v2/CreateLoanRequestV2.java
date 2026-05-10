package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Minimal request body for borrowing a book through the v2 API.
@Schema(
        name = "CreateLoanRequestV2",
        description = "Request body for creating a new loan in API version 2"
)
public record CreateLoanRequestV2(
        @Schema(
                description = "Id of the book to loan",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Book id is required")
        @Min(value = 1, message = "Book id must be >= 1")
        Long bookId
) {
}
