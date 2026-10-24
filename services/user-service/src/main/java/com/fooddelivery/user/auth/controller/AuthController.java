package com.fooddelivery.user.auth.controller;

import com.fooddelivery.user.auth.service.AuthService;
import com.fooddelivery.user.dto.request.LoginRequest;
import com.fooddelivery.user.dto.response.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication Controller.
 * 
 * Public endpoint for user authentication.
 * 
 * POST /api/v1/auth/login - Authenticate user with email and password
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Login endpoint.
     * 
     * POST /api/v1/auth/login
     * 
     * Request body:
     * {
     *   "email": "user@example.com",
     *   "password": "password123"
     * }
     * 
     * Success response (200 OK):
     * {
     *   "accessToken": "...",
     *   "tokenType": "Bearer",
     *   "expiresIn": 3600
     * }
     * 
     * Failure response (401 Unauthorized):
     * Generic authentication failure message (does not expose whether email exists or password is wrong)
     * 
     * @param loginRequest Login credentials
     * @return LoginResponse containing JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug("Login attempt for email: {}", loginRequest.getEmail());
        LoginResponse loginResponse = authService.login(loginRequest);
        log.debug("Login successful for email: {}", loginRequest.getEmail());
        return ResponseEntity.ok(loginResponse);
    }
}
