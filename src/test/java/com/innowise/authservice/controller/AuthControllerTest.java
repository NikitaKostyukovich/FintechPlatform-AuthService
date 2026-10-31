package com.innowise.authservice.controller;

import com.innowise.authservice.dto.request.LoginRequestDto;
import com.innowise.authservice.dto.request.RefreshRequestDto;
import com.innowise.authservice.dto.request.RegisterRequestDto;
import com.innowise.authservice.dto.response.LoginResponseDto;
import com.innowise.authservice.role.Role;
import com.innowise.authservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthControllerTest {

    @Test
    void register_ShouldReturnCreatedWithUserId() {
        AuthService dummyAuthService = new AuthService(null, null, null, null, null, null) {
            @Override
            public Long register(RegisterRequestDto dto) {
                return 10L;
            }
        };

        AuthController controller = new AuthController(dummyAuthService);

        RegisterRequestDto requestDto = new RegisterRequestDto(
                "testuser", "password123", "test@example.com", "Ivan", "Ivanov", LocalDate.of(2000, 1, 1)
        );

        ResponseEntity<Long> response = controller.register(requestDto);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(10L, response.getBody());
    }
}