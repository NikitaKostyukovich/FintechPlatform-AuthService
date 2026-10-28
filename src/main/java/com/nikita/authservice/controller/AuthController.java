package com.nikita.authservice.controller;

import com.nikita.authservice.dto.*;
import com.nikita.authservice.entity.Credential;
import com.nikita.authservice.entity.RefreshToken;
import com.nikita.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Long> register(@Valid @RequestBody RegisterRequestDto dto){
        Long userId = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto dto){
       return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@Valid @RequestBody RefreshRequestDto dto){
        return ResponseEntity.ok(authService.refresh(dto.refreshToken()));
    }

}
