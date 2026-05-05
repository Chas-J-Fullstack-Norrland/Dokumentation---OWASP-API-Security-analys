package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;

// v2 currently mirrors the author summary shape but keeps a separate contract for future changes.
@Schema(name = "AuthorDtoV2", description = "Author response for API version 2")
public record AuthorDtoV2(
        @Schema(description = "Author id", example = "1")
        Long id,

        @Schema(description = "Author name", example = "Christopher Paolini")
        String name,

        @Schema(description = "Number of books written by the author", example = "3")
        int numberOfBooks
) {
}
