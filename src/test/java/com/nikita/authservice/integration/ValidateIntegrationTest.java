package com.nikita.authservice.integration;

import com.nikita.authservice.dto.LoginResponseDto;
import com.nikita.authservice.dto.ValidateResponseDto;
import com.nikita.authservice.role.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidateIntegrationTest extends AbstractAuthIntegrationTest {

    @Test
    void accessToken_returnsUserIdAndRole() {
        LoginResponseDto response = loginAsIvan();

        ValidateResponseDto result = authService.validate(response.accessToken());

        assertTrue(result.valid());
        assertEquals(1L, result.userId());
        assertEquals(Role.USER, result.role());
    }

    @Test
    void refreshToken_isNotValidAsAccessToken() {
        LoginResponseDto response = loginAsIvan();

        ValidateResponseDto result = authService.validate(response.refreshToken());

        assertFalse(result.valid());
        assertNull(result.userId());
        assertNull(result.role());
    }

    @Test
    void garbage_isNotValid() {
        assertFalse(authService.validate("not-a-token").valid());
    }
}