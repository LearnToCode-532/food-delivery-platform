package com.fooddelivery.user.exception;

public class UserAlreadyExistsException extends RuntimeException {
    
    public UserAlreadyExistsException(String message) {
        super(message);
    }
    
    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public static UserAlreadyExistsException withEmail(String email) {
        return new UserAlreadyExistsException("User with email '" + email + "' already exists");
    }
    
    public static UserAlreadyExistsException withPhoneNumber(String phoneNumber) {
        return new UserAlreadyExistsException("User with phone number '" + phoneNumber + "' already exists");
    }
}
