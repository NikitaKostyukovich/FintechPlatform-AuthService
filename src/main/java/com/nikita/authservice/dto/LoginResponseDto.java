package com.nikita.authservice.dto;

public record LoginResponseDto(
        String accessToken,
        String refreshToken) {
}
