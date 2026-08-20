package com.fooddelivery.user.auth.service.impl;

import com.fooddelivery.user.auth.service.AuthService;
import com.fooddelivery.user.dto.request.LoginRequest;
import com.fooddelivery.user.dto.response.LoginResponse;
import com.fooddelivery.user.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * Authentication Service implementation.
 * 
 * Responsibilities:
 * 1. Accept login credentials
 * 2. Authenticate through Spring Security
 * 3. Use AuthenticationManager for credential verification
 * 4. Generate JWT upon successful authentication
 * 5. Return LoginResponse
 * 
 * Does NOT:
 * - Manually compare plaintext passwords
 * - Put login logic into UserService
 * - Put login logic into UserController
 * 
 * Password verification happens through Spring Security's authentication mechanism.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        log.debug("Attempting authentication for user: {}", loginRequest.getEmail());
        
        // Authenticate using Spring Security
        // This will use AuthenticationProvider -> UserDetailsService -> PasswordEncoder
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );
        
        log.debug("Authentication successful for user: {}", loginRequest.getEmail());
        
        // Generate JWT token
        String accessToken = jwtService.generateToken(loginRequest.getEmail());
        long expiresIn = jwtService.getExpirationInSeconds();
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
