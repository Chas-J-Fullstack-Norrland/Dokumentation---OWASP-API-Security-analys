package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// v2 requires genre when a new book is created.
@Schema(name = "CreateBookRequestV2", description = "Request body for creating a book in API version 2")
public record CreateBookRequestV2(
        @Schema(description = "Book title", example = "Eragon", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Title is required")
        String title,

        @Schema(description = "Id of the author", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Author id is required")
        Long authorId,

        @Schema(description = "Book genre", example = "Fantasy", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Genre is required")
        String genre,

        @Schema(description = "ISBN number", example = "9780000000003", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "ISBN is required")
        String isbn,

        @Schema(description = "Publication year", example = "2005", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 0, message = "Publication year must be 0 or later")
        int publicationYear
) {
}
