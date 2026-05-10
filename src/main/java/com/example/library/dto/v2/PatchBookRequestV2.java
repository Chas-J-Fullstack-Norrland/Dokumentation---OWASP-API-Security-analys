package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

// PATCH uses nullable fields so clients can update only the book properties they want to change.
@Schema(name = "PatchBookRequestV2", description = "Request body for partially updating a book in API version 2")
public record PatchBookRequestV2(
        @Schema(description = "Updated book title", example = "Eragon", nullable = true)
        String title,

        @Schema(description = "Updated author id", example = "1", nullable = true)
        @Min(value = 1, message = "Author id must be >= 1")
        Long authorId,

        @Schema(description = "Updated book genre", example = "Fantasy", nullable = true)
        String genre,

        @Schema(description = "Updated ISBN number", example = "9780000000003", nullable = true)
        String isbn,

        @Schema(description = "Updated publication year", example = "2005", nullable = true)
        @Min(value = 0, message = "Publication year must be 0 or later")
        Integer publicationYear
) {
}