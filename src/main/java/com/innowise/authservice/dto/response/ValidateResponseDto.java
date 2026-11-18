package com.innowise.authservice.dto.response;

import com.innowise.authservice.role.Role;

public record ValidateResponseDto(
        boolean valid,
        Long userId,
        Role role
) {
}
