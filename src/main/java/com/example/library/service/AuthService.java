package com.example.library.service;


import com.example.library.dto.auth.LoginRequest;
import com.example.library.dto.auth.LoginResponse;
import com.example.library.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final Long expirationSeconds;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds
    ){
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.expirationSeconds = expirationSeconds;
    }

    public LoginResponse login(LoginRequest request){
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(),request.password())
        );

    String role = auth.getAuthorities().stream()
            .findFirst()
            .map(GrantedAuthority::getAuthority)
            .orElse("ROLE_USER");
    String token = jwtService.generateToken(request.username(), role);
        return new  LoginResponse(token,"Bearer",expirationSeconds);
    }


}
