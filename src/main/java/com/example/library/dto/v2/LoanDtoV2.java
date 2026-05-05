package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

// v2 keeps the same core loan data while reserving room for future response changes.
@Schema(name = "LoanDtoV2", description = "Loan response for API version 2")
public record LoanDtoV2(
        @Schema(description = "Loan id", example = "1")
        Long id,

        @Schema(description = "Book id", example = "1")
        Long bookId,

        @Schema(description = "Book title", example = "Eragon")
        String bookTitle,

        @Schema(description = "Date when the loan was created", example = "2026-04-16")
        LocalDate loanDate,

        @Schema(description = "Return date, null if the book is still on loan", example = "2026-04-20", nullable = true)
        LocalDate returnDate

) {
}
