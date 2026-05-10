package com.example.library.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoryRefreshToken {

    private record TokenMeta(String username, Instant expiresAt, boolean revoked){}

    private final Map<String, TokenMeta> tokens = new ConcurrentHashMap<>();

    public void save(String refreshToken, String username, Instant expiresAt) {
        tokens.put(refreshToken, new TokenMeta(username, expiresAt, false));
    }

    public boolean isValid(String refreshToken, String username) {
        TokenMeta meta = tokens.get(refreshToken);
        return meta != null
                && !meta.revoked
                && meta.username.equals(username)
                && meta.expiresAt.isAfter(Instant.now());
    }

    public void revoke(String refreshToken) {
        TokenMeta meta = tokens.get(refreshToken);
        if (meta != null) {
            tokens.put(refreshToken, new TokenMeta(meta.username, meta.expiresAt, true));
        }
    }
}


