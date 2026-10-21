package com.fooddelivery.user.controller;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import com.fooddelivery.user.domain.service.UserService;
import com.fooddelivery.user.dto.request.CreateUserRequest;
import com.fooddelivery.user.dto.request.UpdateUserRequest;
import com.fooddelivery.user.dto.response.UserResponse;
import com.fooddelivery.user.exception.UserAlreadyExistsException;
import com.fooddelivery.user.mapper.UserMapper;
import com.fooddelivery.user.security.CustomUserDetailsService;
import com.fooddelivery.user.security.JwtService;

import tools.jackson.databind.ObjectMapper;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private UserService userService;
    
    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    CustomUserDetailsService customUserDetailsService;
    
    private CreateUserRequest createUserRequest;
    private User user;
    private UserResponse userResponse;
    
    @BeforeEach
    void setUp() {
        createUserRequest = CreateUserRequest.builder()
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .password("password123")
                .build();
        
        user = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("hashedPassword123")
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        userResponse = UserResponse.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .status(UserStatus.ACTIVE)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
    
    @Test
    void testCreateUserSuccess() throws Exception {
        // Arrange
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("9876543210"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        
        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
        verify(userMapper, times(1)).toResponse(user);
    }
    
    @Test
    void testCreateUserInvalidRequest() throws Exception {
        // Arrange
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .email("invalid-email")
                .firstName("J")
                .lastName("D")
                .phoneNumber("123")
                .password("123")
                .build();
        
        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors").isMap())
                .andReturn();
        
        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }
    
    @Test
    void testCreateUserMissingFields() throws Exception {
        // Arrange
        String incompleteJson = "{}";
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(incompleteJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
        
        verify(userService, never()).createUser(any(CreateUserRequest.class));
    }
    
    @Test
    void testCreateUserDuplicateEmail() throws Exception {
        // Arrange
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(UserAlreadyExistsException.withEmail("john@example.com"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User with email 'john@example.com' already exists"))
                .andExpect(jsonPath("$.path").value("/api/v1/users"));
        
        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }
    
    @Test
    void testCreateUserDuplicatePhone() throws Exception {
        // Arrange
        when(userService.createUser(any(CreateUserRequest.class)))
                .thenThrow(UserAlreadyExistsException.withPhoneNumber("9876543210"));
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("User with phone number '9876543210' already exists"));
        
        verify(userService, times(1)).createUser(any(CreateUserRequest.class));
    }
    
    @Test
    void testGetUserSuccess() throws Exception {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(Optional.of(userResponse));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        authenticateAs(user.getEmail());
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/1").header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer test-token"
                )
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.phoneNumber").value("9876543210"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
        
        verify(userService, times(1)).getUserById(1L);
    }
    
    @Test
    void testGetUserNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(999L)).thenReturn(Optional.empty());
        authenticateAs(user.getEmail());
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/999").header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer test-token"
                )
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User with ID '999' not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/999"));
        
        verify(userService, times(1)).getUserById(999L);
        verify(userMapper, never()).toResponse(any(User.class));
    }
    
    @Test
    void testCreateUserPasswordNotInResponse() throws Exception {
        // Arrange
        when(userService.createUser(any(CreateUserRequest.class))).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        
        // Act
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createUserRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        
        // Assert
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("hashedPassword123");
    }
    
    @Test
    void testGetUserPasswordNotInResponse() throws Exception {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(Optional.of(userResponse));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        authenticateAs(user.getEmail());
        
        // Act
        MvcResult result = mockMvc.perform(get("/api/v1/users/1").header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer test-token"
                )
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        
        // Assert
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("hashedPassword123");
    }

    @Test
    void testUpdateUserUsesAuthenticatedIdentityAndDoesNotExposeSecrets() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("  JOHN@EXAMPLE.COM ");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPhoneNumber("9876543210");
        when(userService.updateUser(eq(1L), eq(user.getEmail()), any(UpdateUserRequest.class)))
                .thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        authenticateAs(user.getEmail());

        MvcResult result = mockMvc.perform(put("/api/v1/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        assertThat(result.getResponse().getContentAsString()).doesNotContain("hashedPassword123");
        verify(userService).updateUser(eq(1L), eq(user.getEmail()), any(UpdateUserRequest.class));
    }

    @Test
    void testDeleteUserUsesAuthenticatedIdentity() throws Exception {
        authenticateAs(user.getEmail());

        mockMvc.perform(delete("/api/v1/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L, user.getEmail());
    }

    @Test
    void testUpdateUserRejectsOwnershipMismatch() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("john@example.com");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPhoneNumber("9876543210");
        when(userService.updateUser(eq(2L), eq(user.getEmail()), any(UpdateUserRequest.class)))
                .thenThrow(new AccessDeniedException("not authorized"));
        authenticateAs(user.getEmail());

        mockMvc.perform(put("/api/v1/users/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testDeleteUserRejectsOwnershipMismatch() throws Exception {
        doThrow(new AccessDeniedException("not authorized"))
                .when(userService).deleteUser(2L, user.getEmail());
        authenticateAs(user.getEmail());

        mockMvc.perform(delete("/api/v1/users/2")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    void testProfileChangesRequireAuthentication() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("john@example.com");
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPhoneNumber("9876543210");

        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/users/1"))
                .andExpect(status().isUnauthorized());

        verify(userService, never()).updateUser(anyLong(), anyString(), any(UpdateUserRequest.class));
        verify(userService, never()).deleteUser(anyLong(), anyString());
    }

    @Test
    void testUpdateUserValidatesEmailAndProfileFields() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("not-an-email");
        request.setFirstName("J");
        request.setLastName("Smith");
        request.setPhoneNumber("123");
        authenticateAs(user.getEmail());

        mockMvc.perform(put("/api/v1/users/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phoneNumber").exists());

        verify(userService, never()).updateUser(anyLong(), anyString(), any(UpdateUserRequest.class));
    }

    private void authenticateAs(String email) {
        when(jwtService.validateToken("test-token")).thenReturn(true);
        when(jwtService.getSubjectFromToken("test-token")).thenReturn(email);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password("test-hash")
                .authorities(java.util.List.of())
                .build();
        when(customUserDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
    }
}
