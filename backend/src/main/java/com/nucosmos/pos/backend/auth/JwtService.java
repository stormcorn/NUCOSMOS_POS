package com.nucosmos.pos.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecretBase64()));
    }

    public String generateToken(String subject, Map<String, Object> claims) {
        Date issuedAt = new Date();
        Date expiresAt = Date.from(calculateExpiresAt().toInstant());

        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(issuedAt)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public OffsetDateTime calculateExpiresAt() {
        return OffsetDateTime.now().plusMinutes(jwtProperties.getAccessTokenMinutes());
    }
}
