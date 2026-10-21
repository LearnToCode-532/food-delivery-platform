package com.fooddelivery.user.controller;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import com.fooddelivery.user.domain.repository.UserRepository;
import com.fooddelivery.user.dto.request.LoginRequest;
import com.fooddelivery.user.dto.request.UpdateUserRequest;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for JWT authentication flow.
 * 
 * Test scenarios:
 * 1. Login with valid credentials returns JWT
 * 2. Protected endpoint with valid JWT returns 200
 * 3. Protected endpoint without JWT returns 401
 * 4. Protected endpoint with invalid JWT returns 401
 * 5. Login endpoint works without JWT
 * 6. Inactive user cannot login
 * 7. Suspended user cannot login
 * 8. Deleted user cannot login
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret-key=test-secret-key-must-be-at-least-256-bits-long-1234567890",
        "jwt.expiration-ms=3600000"
})
class JwtAuthenticationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private User activeUser;
    private User inactiveUser;
    private User suspendedUser;
    private String testPassword;
    private String testEmail;
    
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        
        testEmail = "john@example.com";
        testPassword = "password123";
        
        // Create active user
        activeUser = User.builder()
                .email(testEmail)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .passwordHash(passwordEncoder.encode(testPassword))
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(activeUser);
        
        // Create inactive user
        inactiveUser = User.builder()
                .email("inactive@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("0987654321")
                .passwordHash(passwordEncoder.encode(testPassword))
                .status(UserStatus.INACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(inactiveUser);
        
        // Create suspended user
        suspendedUser = User.builder()
                .email("suspended@example.com")
                .firstName("Bob")
                .lastName("Smith")
                .phoneNumber("5555555555")
                .passwordHash(passwordEncoder.encode(testPassword))
                .status(UserStatus.SUSPENDED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        userRepository.save(suspendedUser);
    }
    
    @Test
    void testLoginWithValidCredentialsReturnsJwt() throws Exception {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }
    
    @Test
    void testProtectedEndpointWithValidJwtReturns200() throws Exception {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();
        
        // Get JWT token
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseBody = loginResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).get("accessToken").asText();
        
        // Act & Assert - Access protected endpoint with JWT
        mockMvc.perform(get("/api/v1/users/" + activeUser.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activeUser.getId()))
                .andExpect(jsonPath("$.email").value(testEmail));
    }
    
    @Test
    void testProtectedEndpointWithoutJwtReturns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/" + activeUser.getId())).andDo(MockMvcResultHandlers.print())
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testProtectedEndpointWithInvalidJwtReturns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/" + activeUser.getId())
                .header("Authorization", "Bearer invalid-token-string"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testProtectedEndpointWithMalformedAuthHeaderReturns401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/" + activeUser.getId())
                .header("Authorization", "NotBearer some-token"))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testLoginEndpointIsPublic() throws Exception {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();
        
        // Act & Assert - Login endpoint should work without existing authentication
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void testEmailChangeRequiresNewJwtButAllowsReauthentication() throws Exception {
        String oldToken = loginAndGetToken(testEmail);
        UpdateUserRequest updateRequest = new UpdateUserRequest();
        updateRequest.setEmail("updated@example.com");
        updateRequest.setFirstName("John");
        updateRequest.setLastName("Doe");
        updateRequest.setPhoneNumber("1234567890");

        mockMvc.perform(put("/api/v1/users/" + activeUser.getId())
                        .header("Authorization", "Bearer " + oldToken)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));

        mockMvc.perform(get("/api/v1/users/" + activeUser.getId())
                        .header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());

        String newToken = loginAndGetToken("updated@example.com");
        mockMvc.perform(get("/api/v1/users/" + activeUser.getId())
                        .header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void testPreviouslyIssuedJwtIsRejectedAfterSoftDeletion() throws Exception {
        String token = loginAndGetToken(testEmail);

        mockMvc.perform(delete("/api/v1/users/" + activeUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        User deletedUser = userRepository.findById(activeUser.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(UserStatus.DELETED, deletedUser.getStatus());

        mockMvc.perform(get("/api/v1/users/" + activeUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/v1/users/" + activeUser.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testInactiveUserCannotLogin() throws Exception {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email("inactive@example.com")
                .password(testPassword)
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testSuspendedUserCannotLogin() throws Exception {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email("suspended@example.com")
                .password(testPassword)
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
    
    @Test
    void testInvalidCredentialsReturns401() throws Exception {
        // Arrange
        LoginRequest loginRequest = LoginRequest.builder()
                .email(testEmail)
                .password("wrongpassword")
                .build();
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    private String loginAndGetToken(String email) throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email(email)
                .password(testPassword)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }
}
