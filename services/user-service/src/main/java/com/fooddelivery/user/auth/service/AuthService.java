package com.fooddelivery.user.auth.service;

import com.fooddelivery.user.dto.request.LoginRequest;
import com.fooddelivery.user.dto.response.LoginResponse;

/**
 * Authentication Service interface.
 * 
 * Handles user authentication and JWT generation.
 */
public interface AuthService {
    
    /**
     * Authenticate user with email and password.
     * 
     * @param loginRequest Login request containing email and password
     * @return LoginResponse containing JWT token
     * @throws org.springframework.security.core.AuthenticationException if authentication fails
     */
    LoginResponse login(LoginRequest loginRequest);
}
