package com.fooddelivery.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JwtService.
 * 
 * Test scenarios:
 * 1. Successful JWT generation with valid subject
 * 2. Generated JWT contains expected subject
 * 3. Generated JWT contains issued-at and expiration claims
 * 4. JWT expiration is set correctly according to configuration
 * 5. JWT signature can be validated
 * 6. JWT validation fails for expired tokens
 * 7. JWT validation fails for invalid signatures
 * 8. JWT does not contain password or passwordHash
 * 9. Subject can be extracted from JWT
 */
class JwtServiceTest {

    private static final String TEST_SECRET =
            "test-secret-key-must-be-at-least-256-bits-long-1234567890";

    private JwtService jwtService;

    private String testSubject;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKeyString", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3_600_000L);
        testSubject = "john@example.com";
    }

    @Test
    void testGenerateToken() {
        // Act
        String token = jwtService.generateToken(testSubject);
        
        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts: header.payload.signature
    }
    
    @Test
    void testGeneratedTokenContainsSubject() {
        // Act
        String token = jwtService.generateToken(testSubject);
        String extractedSubject = jwtService.getSubjectFromToken(token);
        
        // Assert
        assertEquals(testSubject, extractedSubject);
    }
    
    @Test
    void testGeneratedTokenContainsIssuedAtAndExpiration() {
        // Act
        String token = jwtService.generateToken(testSubject);
        Claims claims = jwtService.getAllClaimsFromToken(token);
        
        // Assert
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getIssuedAt().before(claims.getExpiration()));
    }
    
    @Test
    void testTokenExpirationIsSetCorrectly() {
        // Act
        String token = jwtService.generateToken(testSubject);
        Claims claims = jwtService.getAllClaimsFromToken(token);
        long expirationTime = claims.getExpiration().getTime();
        long issuedTime = claims.getIssuedAt().getTime();
        
        // Assert - Expiration should be approximately 1 hour from issue time (with some tolerance)
        long expectedDuration = 3600000; // 1 hour in milliseconds
        long actualDuration = expirationTime - issuedTime;
        assertTrue(Math.abs(actualDuration - expectedDuration) < 1000); // Within 1 second tolerance
    }
    
    @Test
    void testValidateValidToken() {
        // Act
        String token = jwtService.generateToken(testSubject);
        boolean isValid = jwtService.validateToken(token);
        
        // Assert
        assertTrue(isValid);
    }
    
    @Test
    void testValidateInvalidSignature() {
        // Act
        String token = jwtService.generateToken(testSubject);
        // Tamper with the token by changing a character
        String tamperedToken = token.substring(0, token.length() - 1) + "x";
        boolean isValid = jwtService.validateToken(tamperedToken);
        
        // Assert
        assertFalse(isValid);
    }
    
    @Test
    void testValidateMalformedToken() {
        // Act
        String malformedToken = "invalid.token.string";
        boolean isValid = jwtService.validateToken(malformedToken);
        
        // Assert
        assertFalse(isValid);
    }

    @Test
    void testValidateExpiredToken() {
        String expiredToken = Jwts.builder()
                .subject(testSubject)
                .issuedAt(new Date(0))
                .expiration(new Date(1))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertFalse(jwtService.validateToken(expiredToken));
        assertTrue(jwtService.isTokenExpired(expiredToken));
    }
    
    @Test
    void testExtractSubjectFromToken() {
        // Act
        String token = jwtService.generateToken(testSubject);
        String extractedSubject = jwtService.getSubjectFromToken(token);
        
        // Assert
        assertEquals(testSubject, extractedSubject);
    }
    
    @Test
    void testIsTokenExpired() {
        // Act
        String token = jwtService.generateToken(testSubject);
        boolean isExpired = jwtService.isTokenExpired(token);
        
        // Assert
        assertFalse(isExpired);
    }
    
    @Test
    void testGetExpirationInSeconds() {
        // Act
        long expirationSeconds = jwtService.getExpirationInSeconds();
        
        // Assert - Should be 3600 seconds (1 hour)
        assertEquals(3600L, expirationSeconds);
    }
    
    @Test
    void testTokenDoesNotContainSensitiveInformation() {
        // Act
        String token = jwtService.generateToken(testSubject);
        Claims claims = jwtService.getAllClaimsFromToken(token);
        
        // Assert - Verify that no password or passwordHash claims exist
        assertNull(claims.get("password"));
        assertNull(claims.get("passwordHash"));
        assertNull(claims.get("sensitiveData"));
    }
}
