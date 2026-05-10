package com.example.library.service;



import com.example.library.dto.auth.LoginRequest;

import com.example.library.dto.auth.RefreshTokenRequest;
import com.example.library.dto.auth.TokenPairResponse;
import com.example.library.security.MemoryRefreshToken;
import com.example.library.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;


import java.time.Instant;

@Service
public class AuthService {

    private static final long REFRESH_TTL_SECONDS = 1209600L; // 14 days

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final MemoryRefreshToken memoryRefreshToken;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            MemoryRefreshToken memoryRefreshToken
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.memoryRefreshToken = memoryRefreshToken;
    }

    public TokenPairResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_USER");

        String accessToken = jwtService.generateAccessToken(auth.getName(), role);
        String refreshTokenValue = jwtService.generateRefreshToken(auth.getName());

        memoryRefreshToken.save(refreshTokenValue, auth.getName(), Instant.now().plusSeconds(REFRESH_TTL_SECONDS));

        return new TokenPairResponse(
                accessToken,
                refreshTokenValue,
                "Bearer",
                jwtService.getAccessExpirationSeconds()
        );
    }

    public TokenPairResponse refresh(RefreshTokenRequest request) {
        String refreshTokenValue = request.refreshToken();

        if (!jwtService.isValid(refreshTokenValue) || !"refresh".equals(jwtService.extractType(refreshTokenValue))) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String username = jwtService.extractUsername(refreshTokenValue);

        if (!memoryRefreshToken.isValid(refreshTokenValue, username)) {
            throw new BadCredentialsException("Refresh token revoked/expired");
        }

        memoryRefreshToken.revoke(refreshTokenValue);

        String newAccessToken = jwtService.generateAccessToken(username, "ROLE_USER");
        String newRefreshToken = jwtService.generateRefreshToken(username);
        memoryRefreshToken.save(newRefreshToken, username, Instant.now().plusSeconds(REFRESH_TTL_SECONDS));

        return new TokenPairResponse(
                newAccessToken,
                newRefreshToken,
                "Bearer",
                jwtService.getAccessExpirationSeconds()
        );
    }


}
