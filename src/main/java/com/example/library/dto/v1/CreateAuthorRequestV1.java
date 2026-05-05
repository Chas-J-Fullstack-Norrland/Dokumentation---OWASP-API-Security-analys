package com.example.library.dto.v1;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;


// Request contract for creating authors through the v1 API.
@Schema(
        name = "CreateAuthorRequestV1",
        description = "Request body for creating an author in API version 1"
)
public record CreateAuthorRequestV1(
        @Schema(
                description = "Author name",
                example = "Christopher Paolini",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Author name is required")
        String name
) {
}
