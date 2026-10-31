package com.innowise.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ValidateRequestDto(@NotBlank String token) {}