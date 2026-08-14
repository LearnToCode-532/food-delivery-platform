package com.fooddelivery.user.exception;

public class UserNotFoundException extends RuntimeException {
    
    public UserNotFoundException(String message) {
        super(message);
    }
    
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static UserNotFoundException withId(Long userId) {
        return new UserNotFoundException("User with ID '" + userId + "' not found");
    }
    
    public static UserNotFoundException withEmail(String email) {
        return new UserNotFoundException("User with email '" + email + "' not found");
    }
}
