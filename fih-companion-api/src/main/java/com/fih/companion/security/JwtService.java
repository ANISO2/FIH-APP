package com.fih.companion.security;

import com.fih.companion.diagnostics.ConsoleLog;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private static final String TAG = "JWT";

    private final SecretKey key;
    private final long expirationMillis;

    public JwtService(SecurityProperties properties) {
        this.key = Keys.hmacShaKeyFor(
                properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = properties.getJwt().getExpirationMinutes() * 60_000L;
        // Feature 1 — never print the secret; print only its length so a
        // secret-mismatch between two server instances is visible at a glance.
        ConsoleLog.log(TAG, "initialised — HS256 key length=" + properties.getJwt().getSecret().length()
                + " chars, token TTL=" + properties.getJwt().getExpirationMinutes() + " min ("
                + expirationMillis + " ms).");
    }

    public String generate(String username, String role, String displayName) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMillis);
        ConsoleLog.log(TAG, "GENERATE token — subject=" + username + ", role=" + role
                + ", name=" + displayName + ", issuedAt=" + now + ", expiresAt=" + exp + ".");
        try {
            String token = Jwts.builder()
                    .subject(username)
                    .claim("role", role)
                    .claim("name", displayName)
                    .issuedAt(now)
                    .expiration(exp)
                    .signWith(key)
                    .compact();
            ConsoleLog.log(TAG, "GENERATE ok — subject=" + username + ", token length=" + token.length()
                    + " chars (signed HS256).");
            return token;
        } catch (RuntimeException ex) {
            ConsoleLog.error(TAG, "GENERATE FAILED — subject=" + username
                    + ", reason=" + ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
            throw ex;
        }
    }

    public Claims parse(String token) {

        ConsoleLog.log(TAG, "PARSE token — verifying signature & expiry (token length="
                + (token == null ? 0 : token.length()) + " chars).");
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        ConsoleLog.log(TAG, "PARSE ok — subject=" + claims.getSubject()
                + ", role=" + claims.get("role") + ", expiresAt=" + claims.getExpiration() + ".");
        return claims;
    }
}
