package com.fooddelivery.user.controller;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import com.fooddelivery.user.domain.service.UserService;
import com.fooddelivery.user.dto.request.CreateUserRequest;
import com.fooddelivery.user.dto.response.UserResponse;
import com.fooddelivery.user.exception.UserAlreadyExistsException;
import com.fooddelivery.user.mapper.UserMapper;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(UserController.class)
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private UserService userService;
    
    @MockitoBean
    private UserMapper userMapper;
    
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
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/1")
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
        verify(userMapper, times(1)).toResponse(user);
    }
    
    @Test
    void testGetUserNotFound() throws Exception {
        // Arrange
        when(userService.getUserById(999L)).thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/users/999")
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
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        
        // Act
        MvcResult result = mockMvc.perform(get("/api/v1/users/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        
        // Assert
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("password");
        assertThat(responseBody).doesNotContain("passwordHash");
        assertThat(responseBody).doesNotContain("hashedPassword123");
    }
}
