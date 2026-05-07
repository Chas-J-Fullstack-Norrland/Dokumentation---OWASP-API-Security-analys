package com.example.library.dto.auth;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expersIn
) {
}
