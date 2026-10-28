package com.nikita.authservice.dto;

import com.nikita.authservice.role.Role;

public record ValidateResponseDto(
        boolean valid,
        Long userId,
        Role role
) {
}
