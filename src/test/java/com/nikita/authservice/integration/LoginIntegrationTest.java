package com.nikita.authservice.integration;

import com.nikita.authservice.dto.LoginRequestDto;
import com.nikita.authservice.dto.LoginResponseDto;
import com.nikita.authservice.entity.RefreshToken;
import com.nikita.authservice.exception.InvalidCredentialsException;
import com.nikita.authservice.role.Role;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginIntegrationTest extends AbstractAuthIntegrationTest {

    @Test
    void correctPassword_returnsTokensAndStoresRefreshJti() {
        LoginResponseDto response = loginAsIvan();

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());

        String jti = jwtService.parse(response.refreshToken()).getId();
        List<RefreshToken> stored = refreshTokenRepository.findAll();
        assertEquals(1, stored.size());
        assertEquals(jti, stored.get(0).getJti());
        assertEquals(1L, stored.get(0).getUserId());
    }

    @Test
    void tokensCarryUserIdAndRoleFromDatabase() {
        saveCredential(5L, "admin", "adminPass1", Role.ADMIN);

        LoginResponseDto response = authService.login(new LoginRequestDto("admin", "adminPass1"));

        Claims claims = jwtService.parse(response.accessToken());
        assertEquals(5L, ((Number) claims.get("user_id")).longValue());
        assertEquals("ADMIN", claims.get("role", String.class));
    }

    @Test
    void wrongPassword_throwsInvalidCredentials() {
        saveCredential(1L, "ivan", "secret123", Role.USER);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequestDto("ivan", "wrong-password")));
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    void unknownLogin_throwsInvalidCredentials() {
        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequestDto("nobody", "whatever")));
    }

    @Test
    void passwordIsStoredAsBcryptHash() {
        saveCredential(1L, "ivan", "secret123", Role.USER);

        String stored = credentialRepository.findByLogin("ivan").orElseThrow().getPasswordHash();

        assertNotEquals("secret123", stored);
        assertTrue(stored.startsWith("$2"));
    }
}