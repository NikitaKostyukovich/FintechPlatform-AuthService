package com.innowise.authservice.exception;

public class UserServiceBadRequestException extends RuntimeException {
    public UserServiceBadRequestException(String message) {
        super(message);
    }
}
