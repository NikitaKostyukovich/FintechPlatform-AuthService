package com.innowise.authservice.controller;

import com.innowise.authservice.dto.request.LoginRequestDto;
import com.innowise.authservice.dto.response.LoginResponseDto;
import com.innowise.authservice.dto.request.RefreshRequestDto;
import com.innowise.authservice.dto.request.RegisterRequestDto;
import com.innowise.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Long> register(@Valid @RequestBody RegisterRequestDto dto){
        log.info("Received POST request to /api/auth/register for login: {}", dto.login());

        Long userId = authService.register(dto);

        log.info("Successfully processed registration request, returned user ID: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto dto){
        log.info("Received POST request to /api/auth/login for login: {}", dto.login());

        LoginResponseDto response = authService.login(dto);

        log.info("Successfully processed login request for login: {}", dto.login());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@Valid @RequestBody RefreshRequestDto dto){
        log.info("Received POST request to /api/auth/refresh");

        LoginResponseDto response = authService.refresh(dto.refreshToken());

        log.info("Successfully processed token refresh request");
        return ResponseEntity.ok(response);
    }
}