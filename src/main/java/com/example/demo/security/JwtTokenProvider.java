package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:mySecretKeyForJWTSigningThatIsAtLeast32CharactersLong}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration:900000}") // 15 минут
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800000}") // 7 дней
    private long refreshTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ===== Генерация токенов =====

    public String generateAccessToken(String username, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        claims.put("email", email);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        // делаем каждый refresh-токен уникальным
        claims.put("jti", UUID.randomUUID().toString());

        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    // ===== Валидация =====

    /** Базовая проверка подписи и срока жизни */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean validateAccessToken(String token) {
        if (validateToken(token)) {
            Claims claims = getAllClaimsFromToken(token);
            return "access".equals(claims.get("type"));
        }
        return false;
    }

    public boolean validateRefreshToken(String token) {
        if (validateToken(token)) {
            Claims claims = getAllClaimsFromToken(token);
            return "refresh".equals(claims.get("type"));
        }
        return false;
    }

    // ===== Извлечение данных =====

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
