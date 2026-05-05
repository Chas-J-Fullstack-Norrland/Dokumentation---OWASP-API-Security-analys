package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;

// v2 expands the book payload with genre and computed availability.
@Schema(name = "BookDtoV2", description = "Book response for API version 2")
public record BookDtoV2(
        @Schema(description = "Book id", example = "1")
        Long id,

        @Schema(description = "Book title", example = "Eragon")
        String title,

        @Schema(description = "Author name", example = "Christopher Paolini")
        String authorName,

        @Schema(description = "Book genre", example = "Fantasy")
        String genre,

        @Schema(description = "ISBN number", example = "9780000000003")
        String isbn,

        @Schema(description = "Publication year", example = "2005")
        int publicationYear,

        @Schema(description = "Whether the book is currently available to loan", example = "true")
        boolean available
) {
}
