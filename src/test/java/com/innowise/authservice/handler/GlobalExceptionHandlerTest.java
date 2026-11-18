package com.innowise.authservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleUserAlreadyExists() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("User already exists");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserAlreadyExists(ex);

        assertEquals(409, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("User already exists", response.getBody().message());
    }

    @Test
    void handleUserServiceException() {
        UserServiceException ex = new UserServiceException("Service unavailable");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserServiceException(ex);

        assertEquals(502, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Service unavailable", response.getBody().message());
    }

    @Test
    void handleInvalidCredentials() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Wrong password");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidCredentials(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Wrong password", response.getBody().message());
    }

    @Test
    void handleInvalidToken() {
        InvalidTokenException ex = new InvalidTokenException("Token expired");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidToken(ex);

        assertEquals(401, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("Token expired", response.getBody().message());
    }
}