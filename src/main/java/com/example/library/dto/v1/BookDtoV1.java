package com.example.library.dto.v1;


import io.swagger.v3.oas.annotations.media.Schema;

// v1 exposes the original book fields and intentionally omits genre and availability.
@Schema(name = "BookDtoV1", description = "Book response for API version 1")
public record BookDtoV1(
        @Schema(description = "Book id", example = "1")
        Long id,

        @Schema(description = "Book title", example = "Eragon")
        String title,

        @Schema(description = "Author name", example = "Christopher Paolini")
        String authorName,

        @Schema(description = "ISBN number", example = "9780000000003")
        String isbn,

        @Schema(description = "Publication year", example = "2005")
        Integer publicationYear
) {
}
