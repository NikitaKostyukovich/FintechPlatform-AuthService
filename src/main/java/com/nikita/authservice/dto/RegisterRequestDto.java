package com.nikita.authservice.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record RegisterRequestDto(
        @NotBlank @Size(max=100) String login,
        @NotBlank @Size(min = 8, max=50) String password,
        @NotBlank @Email String email,
        @NotBlank @Size(max = 25) String name,
        @NotBlank @Size(max = 50) String surname,
        @NotNull @Past LocalDate birthDate
) {

}
