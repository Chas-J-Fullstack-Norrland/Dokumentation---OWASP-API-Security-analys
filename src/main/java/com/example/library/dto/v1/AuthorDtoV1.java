package com.example.library.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;

// v1 returns a compact author summary instead of embedding the full book list.
@Schema(name = "AuthorDtoV1", description = "Author response for API version 1")
public record AuthorDtoV1(
        @Schema(description = "Author id", example = "1")
        Long id,

        @Schema(description = "Author name", example = "Christopher Paolini")
        String name,

        @Schema(description = "Number of books written by the author", example = "4")
        int numberOfBooks
) {
}
