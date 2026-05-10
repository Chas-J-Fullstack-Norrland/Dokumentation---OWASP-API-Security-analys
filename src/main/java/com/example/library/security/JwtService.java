package com.example.library.security;


import com.example.library.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Getter
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpirationSeconds;
    private final long refreshExpirationSeconds;

    public JwtService(JwtProperties props) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.secret()));
        this.accessExpirationSeconds = props.accessExpirationSeconds();
        this.refreshExpirationSeconds = props.refreshExpirationSeconds();
    }
    public String generateAccessToken(String username, String role) {
        return buildToken(username, "access", accessExpirationSeconds, Map.of("role", role));
    }

    public String generateRefreshToken(String username) {
        return buildToken(username, "refresh", refreshExpirationSeconds, Map.of("jti", UUID.randomUUID().toString()));
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public String extractType(String token) {
        return parse(token).get("type", String.class);
    }
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }


    private String buildToken(String username, String type, long expSeconds, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expSeconds);

        return Jwts.builder()
                .subject(username)
                .claims(claims)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }



    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
