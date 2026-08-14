package com.fooddelivery.user.domain.service.impl;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.enums.UserStatus;
import com.fooddelivery.user.domain.repository.UserRepository;
import com.fooddelivery.user.domain.service.UserService;
import com.fooddelivery.user.dto.request.CreateUserRequest;
import com.fooddelivery.user.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public User createUser(CreateUserRequest createUserRequest) {
        log.info("Creating user with email: {}", createUserRequest.getEmail());
        
        // Normalize email: trim and lowercase
        String normalizedEmail = normalizeEmail(createUserRequest.getEmail());
        
        // Check for duplicate email
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("User creation failed - email already exists: {}", normalizedEmail);
            throw UserAlreadyExistsException.withEmail(normalizedEmail);
        }
        
        // Check for duplicate phone number
        if (userRepository.existsByPhoneNumber(createUserRequest.getPhoneNumber())) {
            log.warn("User creation failed - phone number already exists: {}", createUserRequest.getPhoneNumber());
            throw UserAlreadyExistsException.withPhoneNumber(createUserRequest.getPhoneNumber());
        }
        
        // Hash password
        String passwordHash = passwordEncoder.encode(createUserRequest.getPassword());
        
        // Create user with ACTIVE status
        User user = User.builder()
                .email(normalizedEmail)
                .firstName(createUserRequest.getFirstName())
                .lastName(createUserRequest.getLastName())
                .phoneNumber(createUserRequest.getPhoneNumber())
                .passwordHash(passwordHash)
                .status(UserStatus.ACTIVE)
                .build();
        
        // Save to database
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());
        
        return savedUser;
    }
    
    @Override
    public Optional<User> getUserById(Long userId) {
        log.debug("Fetching user by ID: {}", userId);
        return userRepository.findById(userId);
    }
    
    @Override
    public Optional<User> getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        log.debug("Fetching user by email: {}", normalizedEmail);
        return userRepository.findByEmail(normalizedEmail);
    }
    
    @Override
    public boolean emailExists(String email) {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.existsByEmail(normalizedEmail);
    }
    
    @Override
    public boolean phoneNumberExists(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }
    
    /**
     * Normalize email: trim whitespace and convert to lowercase.
     * 
     * @param email the email to normalize
     * @return the normalized email
     */
    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
