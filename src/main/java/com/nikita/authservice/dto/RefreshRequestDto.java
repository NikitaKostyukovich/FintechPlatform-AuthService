package com.nikita.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDto(
        @NotBlank String refreshToken
) {
}
