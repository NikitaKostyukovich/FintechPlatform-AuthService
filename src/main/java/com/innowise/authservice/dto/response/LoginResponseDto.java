package com.innowise.authservice.dto.response;

public record LoginResponseDto(
        String accessToken,
        String refreshToken) {
}
