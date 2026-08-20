package com.fooddelivery.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Service for token generation, validation, and claim extraction.
 * 
 * Minimal claims:
 * - subject (sub): user identity (email)
 * - issued-at (iat): token creation time
 * - expiration (exp): token expiration time
 * 
 * Does NOT include:
 * - password
 * - passwordHash
 * - sensitive information
 */
@Service
public class JwtService {
    
    @Value("${jwt.secret-key}")
    private String secretKeyString;
    
    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;
    
    /**
     * Generate a JWT token for the given subject (email).
     * 
     * @param subject user email
     * @return JWT token
     */
    public String generateToken(String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * Validate JWT token signature and expiration.
     * 
     * @param token JWT token
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract subject (email) from JWT token.
     * 
     * @param token JWT token
     * @return subject claim
     */
    public String getSubjectFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }
    
    /**
     * Extract all claims from JWT token.
     * 
     * @param token JWT token
     * @return claims
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
    }
    
    /**
     * Check if token is expired.
     * 
     * @param token JWT token
     * @return true if expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expirationDate = getAllClaimsFromToken(token).getExpiration();
            return expirationDate.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Get JWT expiration in seconds.
     * 
     * @return expiration time in seconds
     */
    public long getExpirationInSeconds() {
        return jwtExpirationMs / 1000;
    }
    
    /**
     * Create signing key from secret key string.
     * Uses HS256 algorithm which requires 256-bit (32 bytes) minimum.
     * 
     * @return SecretKey for signing
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
