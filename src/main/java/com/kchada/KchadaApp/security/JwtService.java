package com.kchada.KchadaApp.security;

import com.kchada.KchadaApp.user.UserDocument;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final Duration TOKEN_DURATION =
            Duration.ofDays(7);

    private final SecretKey secretKey;

    public JwtService(
            @Value("${kchada.jwt.secret-base64}")
            String secretBase64) {

        byte[] keyBytes = Base64.getDecoder()
                .decode(secretBase64);

        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "La clave JWT debe tener al menos 256 bits"
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDocument user) {

        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.id())
                .claim("ver", user.tokenVersion())
                .issuedAt(Date.from(now))
                .expiration(
                        Date.from(now.plus(TOKEN_DURATION))
                )
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims validateToken(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return TOKEN_DURATION.toSeconds();
    }
}