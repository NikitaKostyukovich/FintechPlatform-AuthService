package com.nikita.authservice.dto;

import java.time.LocalDate;
import java.util.List;

public record AuthResponseDto(
        Long id,
        String name,
        String surname,
        LocalDate birthDate,
        String email,
        boolean active
) {
}
