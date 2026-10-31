package com.innowise.authservice.dto.response;

import java.time.LocalDate;

public record AuthResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active
) {
}
