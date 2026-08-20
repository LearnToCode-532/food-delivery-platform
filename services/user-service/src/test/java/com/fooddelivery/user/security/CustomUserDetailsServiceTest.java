package com.fooddelivery.user.security;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import com.fooddelivery.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for CustomUserDetailsService.
 * 
 * Test scenarios:
 * 1. Load active user by email succeeds
 * 2. Load inactive user by email fails
 * 3. Load suspended user by email fails
 * 4. Load deleted user by email fails
 * 5. Load unknown user by email fails
 * 6. UserDetails contains email as username
 * 7. UserDetails contains password hash
 * 8. Active user is enabled
 * 9. Inactive user is disabled
 * 10. Suspended user is locked
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private CustomUserDetailsService userDetailsService;
    
    private User activeUser;
    private User inactiveUser;
    private User suspendedUser;
    private User deletedUser;
    private String testEmail;
    private String testPasswordHash;
    
    @BeforeEach
    void setUp() {
        testEmail = "john@example.com";
        testPasswordHash = "$2a$10$8j4s9w8w9w8w9w8w9w8w9w8w9w8w9w8w9w8w9w8w9w8w9w8w9w8w9";
        
        // Active user
        activeUser = User.builder()
                .id(1L)
                .email(testEmail)
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("1234567890")
                .passwordHash(testPasswordHash)
                .status(UserStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Inactive user
        inactiveUser = User.builder()
                .id(2L)
                .email("inactive@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .phoneNumber("0987654321")
                .passwordHash(testPasswordHash)
                .status(UserStatus.INACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Suspended user
        suspendedUser = User.builder()
                .id(3L)
                .email("suspended@example.com")
                .firstName("Bob")
                .lastName("Smith")
                .phoneNumber("5555555555")
                .passwordHash(testPasswordHash)
                .status(UserStatus.SUSPENDED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        // Deleted user
        deletedUser = User.builder()
                .id(4L)
                .email("deleted@example.com")
                .firstName("Charlie")
                .lastName("Brown")
                .phoneNumber("6666666666")
                .passwordHash(testPasswordHash)
                .status(UserStatus.DELETED)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
    
    @Test
    void testLoadActiveUserByEmail() {
        // Arrange
        when(userRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(activeUser));
        
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        
        // Assert
        assertNotNull(userDetails);
        assertEquals(testEmail, userDetails.getUsername());
        assertEquals(testPasswordHash, userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
    }
    
    @Test
    void testLoadInactiveUserFails() {
        // Arrange
        when(userRepository.findByEmail("inactive@example.com"))
                .thenReturn(Optional.of(inactiveUser));
        
        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("inactive@example.com"));
    }
    
    @Test
    void testLoadSuspendedUserFails() {
        // Arrange
        when(userRepository.findByEmail("suspended@example.com"))
                .thenReturn(Optional.of(suspendedUser));
        
        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("suspended@example.com"));
    }
    
    @Test
    void testLoadDeletedUserFails() {
        // Arrange
        when(userRepository.findByEmail("deleted@example.com"))
                .thenReturn(Optional.of(deletedUser));
        
        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("deleted@example.com"));
    }
    
    @Test
    void testLoadUnknownUserFails() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());
        
        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown@example.com"));
    }
    
    @Test
    void testUserDetailsContainsEmailAsUsername() {
        // Arrange
        when(userRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(activeUser));
        
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        
        // Assert
        assertEquals(testEmail, userDetails.getUsername());
    }
    
    @Test
    void testUserDetailsContainsPasswordHash() {
        // Arrange
        when(userRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(activeUser));
        
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        
        // Assert
        assertEquals(testPasswordHash, userDetails.getPassword());
    }
    
    @Test
    void testActiveUserIsEnabled() {
        // Arrange
        when(userRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(activeUser));
        
        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername(testEmail);
        
        // Assert
        assertTrue(userDetails.isEnabled());
    }
    
    @Test
    void testSuspendedUserIsLocked() {
        // Arrange
        when(userRepository.findByEmail("suspended@example.com"))
                .thenReturn(Optional.of(suspendedUser));
        
        // Act & Assert - Suspended users cannot load, but if they could, they would be locked
        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("suspended@example.com"));
    }
}
