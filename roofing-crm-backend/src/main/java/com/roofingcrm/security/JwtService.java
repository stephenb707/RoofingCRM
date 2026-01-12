package com.roofingcrm.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    public String generateToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getExpirationSeconds());

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        var jwt = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token);

        String subject = jwt.getPayload().getSubject();
        String email = jwt.getPayload().get("email", String.class);
        UUID userId = UUID.fromString(subject);

        return new AuthenticatedUser(userId, email);
    }
}
