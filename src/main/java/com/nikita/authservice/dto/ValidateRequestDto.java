package com.nikita.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ValidateRequestDto(@NotBlank String token) {}