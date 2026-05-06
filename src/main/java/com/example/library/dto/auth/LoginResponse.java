package com.example.library.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginResponse(
        @NotBlank String accessToken,
        @NotBlank String tokenType,
        Long expireIn
) {
}
