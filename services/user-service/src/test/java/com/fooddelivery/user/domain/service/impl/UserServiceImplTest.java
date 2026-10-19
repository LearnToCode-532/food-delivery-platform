package com.fooddelivery.user.domain.service.impl;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import com.fooddelivery.user.domain.repository.UserRepository;
import com.fooddelivery.user.dto.request.CreateUserRequest;
import com.fooddelivery.user.dto.request.UpdateUserRequest;
import com.fooddelivery.user.dto.response.UserResponse;
import com.fooddelivery.user.event.UserRegisteredEventPublisher;
import com.fooddelivery.user.exception.UserAlreadyExistsException;
import com.fooddelivery.user.exception.UserNotFoundException;
import com.fooddelivery.user.mapper.UserMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserRegisteredEventPublisher userRegisteredEventPublisher;
    
    private CreateUserRequest createUserRequest;
    
    @BeforeEach
    void setUp() {
        createUserRequest = CreateUserRequest.builder()
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .password("password123")
                .build();
    }
    
    @Test
    void testCreateUserSuccess() {
        // Arrange
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        
        User savedUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("hashedPassword123")
                .status(UserStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        User result = userService.createUser(createUserRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals("9876543210", result.getPhoneNumber());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        assertEquals("hashedPassword123", result.getPasswordHash());
        
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, times(1)).existsByPhoneNumber("9876543210");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
        verify(userRegisteredEventPublisher).publishAfterCommit(1L);
    }
    
    @Test
    void testCreateUserEmailNormalization() {
        // Arrange
        createUserRequest.setEmail("  John@EXAMPLE.COM  ");
        
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        
        User savedUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("hashedPassword123")
                .status(UserStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        User result = userService.createUser(createUserRequest);
        
        // Assert
        assertEquals("john@example.com", result.getEmail());
        
        verify(userRepository).existsByEmail("john@example.com");
    }
    
    @Test
    void testCreateUserDuplicateEmail() {
        // Arrange
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);
        
        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.createUser(createUserRequest)
        );
        
        assertEquals("User with email 'john@example.com' already exists", exception.getMessage());
        
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, never()).existsByPhoneNumber(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(userRegisteredEventPublisher, never()).publishAfterCommit(anyLong());
    }
    
    @Test
    void testCreateUserDuplicatePhone() {
        // Arrange
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(true);
        
        // Act & Assert
        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.createUser(createUserRequest)
        );
        
        assertEquals("User with phone number '9876543210' already exists", exception.getMessage());
        
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(userRepository, times(1)).existsByPhoneNumber("9876543210");
        verify(userRepository, never()).save(any(User.class));
        verify(userRegisteredEventPublisher, never()).publishAfterCommit(anyLong());
    }

    @Test
    void testFailedPersistenceDoesNotScheduleRegistrationEvent() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        when(userRepository.save(any(User.class))).thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> userService.createUser(createUserRequest));

        verify(userRegisteredEventPublisher, never()).publishAfterCommit(anyLong());
    }
    
    @Test
    void testCreateUserPasswordIsHashed() {
        // Arrange
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
        
        String hashedPassword = "bcrypt_hashed_password_value";
        when(passwordEncoder.encode("password123")).thenReturn(hashedPassword);
        
        User savedUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash(hashedPassword)
                .status(UserStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        User result = userService.createUser(createUserRequest);
        
        // Assert
        assertEquals(hashedPassword, result.getPasswordHash());
        assertNotEquals("password123", result.getPasswordHash());
        
        verify(passwordEncoder, times(1)).encode("password123");
    }
    
    @Test
    void testCreateUserActiveStatus() {
        // Arrange
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(userRepository.existsByPhoneNumber("9876543210")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword123");
        
        User savedUser = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("hashedPassword123")
                .status(UserStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Act
        User result = userService.createUser(createUserRequest);
        
        // Assert
        assertEquals(UserStatus.ACTIVE, result.getStatus());
    }
    
    @Test
    void testGetUserById() {
        // Arrange
        UserResponse userResponse = UserResponse.builder()
        .id(1L)
        .email("john@example.com")
        .firstName("John")
        .lastName("Doe")
        .phoneNumber("9876543210")
        .status(UserStatus.ACTIVE)
        .build();

        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("hashedPassword123")
                .status(UserStatus.ACTIVE)
                .build();
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(userResponse);
        
        // Act
        Optional<UserResponse> result = userService.getUserById(1L);
        
        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("john@example.com", result.get().getEmail());
        
        verify(userRepository, times(1)).findById(1L);
    }
    
    @Test
    void testGetUserByIdNotFound() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        
        // Act
        Optional<UserResponse> result = userService.getUserById(999L);
        
        // Assert
        assertFalse(result.isPresent());
        
        verify(userRepository, times(1)).findById(999L);
    }
    
    @Test
    void testGetUserByEmail() {
        // Arrange
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("hashedPassword123")
                .status(UserStatus.ACTIVE)
                .build();
        
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        
        // Act
        Optional<User> result = userService.getUserByEmail("john@example.com");
        
        // Assert
        assertTrue(result.isPresent());
        assertTrue(result.isPresent());
        assertEquals("john@example.com", result.get().getEmail());
    }
    @Test
    void testUpdateUserUpdatesProfileFieldsWithoutChangingAccountFields() {
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("existing-hash")
                .status(UserStatus.ACTIVE)
                .build();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");
        request.setPhoneNumber("1234567890");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User result = userService.updateUser(1L, request);

        assertSame(user, result);
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals("1234567890", result.getPhoneNumber());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("existing-hash", result.getPasswordHash());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void testUpdateUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        UpdateUserRequest request = new UpdateUserRequest();

        assertThrows(UserNotFoundException.class, () -> userService.updateUser(999L, request));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteUserRemovesExistingUser() {
        User user = User.builder()
                .id(1L)
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("9876543210")
                .passwordHash("existing-hash")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void testDeleteUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(999L));

        verify(userRepository, never()).delete(any(User.class));
    }
}
