package com.nikita.authservice.integration;

import com.nikita.authservice.dto.LoginResponseDto;
import com.nikita.authservice.exception.InvalidTokenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefreshIntegrationTest extends AbstractAuthIntegrationTest {

    @Test
    void returnsNewPair_andOldTokenCannotBeReused() {
        LoginResponseDto first = loginAsIvan();

        LoginResponseDto second = authService.refresh(first.refreshToken());

        assertNotEquals(first.refreshToken(), second.refreshToken());
        assertNotNull(second.accessToken());
        assertThrows(InvalidTokenException.class, () -> authService.refresh(first.refreshToken()));
        assertNotNull(authService.refresh(second.refreshToken()));
    }

    @Test
    void keepsExactlyOneActiveRefreshTokenPerSession() {
        LoginResponseDto first = loginAsIvan();

        authService.refresh(first.refreshToken());

        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    void withAccessToken_isRejected() {
        LoginResponseDto response = loginAsIvan();

        assertThrows(InvalidTokenException.class, () -> authService.refresh(response.accessToken()));
    }

    @Test
    void withGarbage_isRejected() {
        assertThrows(InvalidTokenException.class, () -> authService.refresh("not-a-token"));
    }

    @Test
    void afterCredentialDeleted_isRejected() {
        LoginResponseDto response = loginAsIvan();

        credentialRepository.deleteAll();

        assertThrows(InvalidTokenException.class, () -> authService.refresh(response.refreshToken()));
    }
}