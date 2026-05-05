package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// Wraps the v2 book list together with explicit version metadata.
@Schema(name = "BookResponseV2", description = "Wrapper response for book lists in API version 2")
public record BookResponseV2(
        @Schema(description = "List of books")
        List<BookDtoV2> data,

        @Schema(description = "API version", example = "v2")
        String version
) {
}
