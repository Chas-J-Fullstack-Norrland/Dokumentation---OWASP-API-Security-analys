package com.example.library.Secuirty;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;




@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationSeconds;

  public JwtService(
          @Value("${app.jwt.secret}") String base64Secret,
          @Value("{app.jwt.expiration-seconds:3600")long expirationSeconds
  ){
      this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
      this.expirationSeconds = expirationSeconds;
  }
  public String generateToken(String username,String role){
      Instant now = Instant.now();
      Instant exp = now.plusSeconds(expirationSeconds);

      return Jwts.builder()
              .subject(username)
              .claims(Map.of("role",role))
              .issuedAt(Date.from(now))
              .expiration(Date.from(exp))
              .signWith(key)
              .compact();
  }
    public String extractUsername(String token) {
        return parse(token).getSubject();
    }


    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
