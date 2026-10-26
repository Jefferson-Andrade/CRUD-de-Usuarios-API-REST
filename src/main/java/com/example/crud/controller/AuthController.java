package com.example.crud.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration lifetime;

    public AuthController(AuthenticationManager authenticationManager, JwtEncoder encoder,
            @Value("${app.security.jwt.issuer:crud-api}") String issuer,
            @Value("${app.security.jwt.ttl-seconds:900}") long ttlSeconds) {
        if (ttlSeconds < 60 || ttlSeconds > 3600) {
            throw new IllegalArgumentException("JWT TTL deve ficar entre 60 e 3600 segundos");
        }
        this.authenticationManager = authenticationManager;
        this.encoder = encoder;
        this.issuer = issuer;
        this.lifetime = Duration.ofSeconds(ttlSeconds);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
            Instant now = Instant.now();
            var claims = JwtClaimsSet.builder().issuer(issuer).subject(auth.getName())
                    .issuedAt(now).expiresAt(now.plus(lifetime)).build();
            var headers = JwsHeader.with(MacAlgorithm.HS256).build();
            return new TokenResponse(encoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue(),
                    "Bearer", lifetime.toSeconds());
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
        }
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}
}
