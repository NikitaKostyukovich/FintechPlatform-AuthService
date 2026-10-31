
package com.innowise.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @NotBlank
        @Size(max = 100)
        String login,

        @NotBlank
        @Size(max = 50)
        String password
) {}
