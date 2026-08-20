package com.fooddelivery.user.auth.service.impl;

import com.fooddelivery.user.dto.request.LoginRequest;
import com.fooddelivery.user.dto.response.LoginResponse;
import com.fooddelivery.user.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for AuthService implementation.
 * 
 * Test scenarios:
 * 1. Valid email/password authenticates successfully
 * 2. Invalid password returns authentication exception
 * 3. Unknown email returns authentication exception
 * 4. JWT is generated upon successful authentication
 * 5. LoginResponse contains accessToken, tokenType, and expiresIn
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    
    @Mock
    private AuthenticationManager authenticationManager;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private Authentication authentication;
    
    @InjectMocks
    private AuthServiceImpl authService;
    
    private LoginRequest validLoginRequest;
    private LoginRequest invalidLoginRequest;
    private String testEmail;
    private String testPassword;
    private String testToken;
    
    @BeforeEach
    void setUp() {
        testEmail = "john@example.com";
        testPassword = "password123";
        testToken = "jwt-token-string";
        
        validLoginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();
        
        invalidLoginRequest = LoginRequest.builder()
                .email(testEmail)
                .password("wrongpassword")
                .build();
    }
    
    @Test
    void testLoginWithValidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(testEmail))
                .thenReturn(testToken);
        when(jwtService.getExpirationInSeconds())
                .thenReturn(3600L);
        
        // Act
        LoginResponse response = authService.login(validLoginRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(testToken, response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(testEmail);
        verify(jwtService).getExpirationInSeconds();
    }
    
    @Test
    void testLoginWithInvalidPassword() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(invalidLoginRequest));
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(anyString());
    }
    
    @Test
    void testLoginWithUnknownEmail() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));
        
        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> authService.login(validLoginRequest));
        
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(anyString());
    }
    
    @Test
    void testLoginResponseContainsAllRequiredFields() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(testEmail))
                .thenReturn(testToken);
        when(jwtService.getExpirationInSeconds())
                .thenReturn(3600L);
        
        // Act
        LoginResponse response = authService.login(validLoginRequest);
        
        // Assert
        assertNotNull(response.getAccessToken());
        assertNotNull(response.getTokenType());
        assertTrue(response.getExpiresIn() > 0);
    }
}
