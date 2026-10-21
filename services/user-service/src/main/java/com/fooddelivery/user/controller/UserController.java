package com.fooddelivery.user.controller;

import com.fooddelivery.user.domain.entity.User;
import com.fooddelivery.user.domain.service.UserService;
import com.fooddelivery.user.dto.request.CreateUserRequest;
import com.fooddelivery.user.dto.request.UpdateUserRequest;
import com.fooddelivery.user.dto.response.UserResponse;
import com.fooddelivery.user.exception.UserNotFoundException;
import com.fooddelivery.user.mapper.UserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    /**
     * Create a new user.
     *
     * @param createUserRequest the user creation request
     * @return created user response with 201 status
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest createUserRequest) {
        
        log.info("POST /api/v1/users - Creating user");
        
        User user = userService.createUser(createUserRequest);
        UserResponse response = userMapper.toResponse(user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Retrieve a user by ID.
     *
     * @param userId the user ID
     * @return user response with 200 status, or 404 if not found
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        
        log.info("GET /api/v1/users/{} - Fetching user", userId);
        
        UserResponse response = userService.getUserById(userId)
                .orElseThrow(() -> UserNotFoundException.withId(userId));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        log.info("PUT /api/v1/users/{} - Updating user", userId);

        User updatedUser = userService.updateUser(userId, authentication.getName(), request);
        UserResponse response = userMapper.toResponse(updatedUser);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId, Authentication authentication) {

        log.info("DELETE /api/v1/users/{} - Deleting user", userId);

        userService.deleteUser(userId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
