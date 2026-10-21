package com.fooddelivery.user.domain.service;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.dto.request.CreateUserRequest;
import com.fooddelivery.user.dto.request.UpdateUserRequest;
import com.fooddelivery.user.dto.response.UserResponse;
import com.fooddelivery.user.exception.UserAlreadyExistsException;
import com.fooddelivery.user.exception.UserNotFoundException;

import java.util.Optional;

public interface UserService {
    
    /**
     * Create a new user.
     * 
     * Performs validation, email normalization, duplicate checking,
     * password hashing, and persistence.
     * 
     * @param createUserRequest the user creation request
     * @return the created user with ACTIVE status
     * @throws UserAlreadyExistsException if email or phone already exists
     */
    User createUser(CreateUserRequest createUserRequest);
    
    /**
     * Retrieve user by ID.
     * 
     * @param userId the user ID
     * @return the userResponse if found
     */
    Optional<UserResponse> getUserById(Long userId);
    
    /**
     * Retrieve user by normalized email.
     * 
     * @param email the user email (will be normalized)
     * @return the user if found
     */
    Optional<User> getUserByEmail(String email);
    
    /**
     * Check if normalized email exists.
     * 
     * @param email the email to check (will be normalized)
     * @return true if email exists, false otherwise
     */
    boolean emailExists(String email);
    
    /**
     * Check if phone number exists.
     * 
     * @param phoneNumber the phone number to check
     * @return true if phone number exists, false otherwise
     */
    boolean phoneNumberExists(String phoneNumber);

    /**
     * Update an existing user.
     * 
     * @param userId the ID of the user to update
     * @param authenticatedEmail the authenticated principal's email
     * @param request the update request containing new details
     * @return the updated user entity
     * @throws UserNotFoundException if user does not exist
     */
    User updateUser(Long userId, String authenticatedEmail, UpdateUserRequest request);

    /**
     * Delete a user by ID.
     *
     * @param userId the user ID
     * @param authenticatedEmail the authenticated principal's email
     * @throws UserNotFoundException if user does not exist
     */
    void deleteUser(Long userId, String authenticatedEmail);
}
