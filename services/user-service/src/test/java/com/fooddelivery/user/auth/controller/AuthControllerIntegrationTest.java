package com.fooddelivery.user.auth.controller;

import com.fooddelivery.user.auth.service.AuthService;
import com.fooddelivery.user.dto.request.LoginRequest;
import com.fooddelivery.user.dto.response.LoginResponse;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * 
 * Test scenarios:
 * 1. Valid login credentials return 200 with JWT
 * 2. Invalid password returns 401 Unauthorized
 * 3. Unknown email returns 401 Unauthorized
 * 4. Invalid login request (blank fields) returns 400 Bad Request
 * 5. Invalid email format returns 400 Bad Request
 * 6. Password is never returned in LoginResponse
 * 7. Login endpoint is public (no authentication required)
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private AuthService authService;
    
    private LoginRequest validLoginRequest;
    private LoginResponse validLoginResponse;
    private String validEmail;
    private String validPassword;
    
    @BeforeEach
    void setUp() {
        validEmail = "john@example.com";
        validPassword = "password123";
        
        validLoginRequest = LoginRequest.builder()
                .email(validEmail)
                .password(validPassword)
                .build();
        
        validLoginResponse = LoginResponse.builder()
                .accessToken("jwt-token-string")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }
    
    @Test
    void testLoginWithValidCredentials() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(validLoginResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token-string"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
        
        verify(authService).login(any(LoginRequest.class));
    }
    
    @Test
    void testLoginWithInvalidPassword() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized());
        
        verify(authService).login(any(LoginRequest.class));
    }
    
    @Test
    void testLoginWithUnknownEmail() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UsernameNotFoundException("User not found"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isUnauthorized());
        
        verify(authService).login(any(LoginRequest.class));
    }
    
    @Test
    void testLoginWithBlankEmail() throws Exception {
        // Arrange
        LoginRequest blankEmailRequest = LoginRequest.builder()
                .email("")
                .password(validPassword)
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blankEmailRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testLoginWithBlankPassword() throws Exception {
        // Arrange
        LoginRequest blankPasswordRequest = LoginRequest.builder()
                .email(validEmail)
                .password("")
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(blankPasswordRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testLoginWithInvalidEmailFormat() throws Exception {
        // Arrange
        LoginRequest invalidEmailRequest = LoginRequest.builder()
                .email("invalid-email")
                .password(validPassword)
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidEmailRequest)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void testLoginResponseDoesNotContainPassword() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(validLoginResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    
    @Test
    void testLoginEndpointIsPublic() throws Exception {
        // Arrange
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(validLoginResponse);
        
        // Act & Assert - No authentication header required
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLoginRequest)))
                .andExpect(status().isOk());
    }
}
