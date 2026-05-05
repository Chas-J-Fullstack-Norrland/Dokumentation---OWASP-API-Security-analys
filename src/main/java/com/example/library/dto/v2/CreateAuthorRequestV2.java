package com.example.library.dto.v2;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


// Request contract for creating authors through the v2 API.
@Schema(
        name = "CreateAuthorRequestV2",
        description = "Request body for creating an author in API version 2"
)
public record CreateAuthorRequestV2(
        @Schema(
                description = "Author name",
                example = "Christopher Paolini",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Author name is required")
        String name

) {
}
