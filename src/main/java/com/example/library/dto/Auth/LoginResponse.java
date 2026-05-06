package com.example.library.dto.Auth;

import jakarta.validation.constraints.NotBlank;

public record LoginResponse(
        @NotBlank String accessToken,
        @NotBlank String tokenType,
        Long expireIn
) {
}
