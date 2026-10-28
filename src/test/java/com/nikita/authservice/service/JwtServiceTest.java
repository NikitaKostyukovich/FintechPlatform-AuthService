package com.nikita.authservice.service;

import com.nikita.authservice.exception.InvalidTokenException;
import com.nikita.authservice.role.Role;
import com.nikita.authservice.support.TestKeys;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final long ACCESS_TTL_MS = 15 * 60 * 1000L;
    private static final long REFRESH_TTL_MS = 7 * 24 * 60 * 60 * 1000L;

    @TempDir
    Path tempDir;

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = newService("main", ACCESS_TTL_MS, REFRESH_TTL_MS);
    }

    private JwtService newService(String keyDirName, long accessTtl, long refreshTtl) throws Exception {
        TestKeys.KeyFiles keys = TestKeys.writeKeyFiles(tempDir.resolve(keyDirName));
        return new JwtService(
                keys.privateKey().toString(),
                keys.publicKey().toString(),
                accessTtl,
                refreshTtl);
    }

    private static long userIdOf(Claims claims) {
        return ((Number) claims.get("user_id")).longValue();
    }


    @Test
    void accessToken_containsUserIdRoleAndType() {
        String token = jwtService.generateAccessToken(42L, Role.USER);

        Claims claims = jwtService.parse(token);

        assertEquals(42L, userIdOf(claims));
        assertEquals("USER", claims.get("role", String.class));
        assertEquals(JwtService.TYPE_ACCESS, claims.get("token_type", String.class));
        assertNotNull(claims.getExpiration());
    }

    @Test
    void accessToken_forAdmin_containsAdminRole() {
        String token = jwtService.generateAccessToken(1L, Role.ADMIN);

        assertEquals("ADMIN", jwtService.parse(token).get("role", String.class));
    }

    @Test
    void refreshToken_hasRefreshTypeAndJti() {
        String token = jwtService.generateRefreshToken(7L, Role.USER);

        Claims claims = jwtService.parse(token);

        assertEquals(JwtService.TYPE_REFRESH, claims.get("token_type", String.class));
        assertEquals(7L, userIdOf(claims));
        assertNotNull(claims.getId());
    }

    @Test
    void refreshToken_livesLongerThanAccessToken() {
        Claims access = jwtService.parse(jwtService.generateAccessToken(1L, Role.USER));
        Claims refresh = jwtService.parse(jwtService.generateRefreshToken(1L, Role.USER));

        assertTrue(refresh.getExpiration().after(access.getExpiration()));
    }

    @Test
    void everyToken_hasUniqueJti() {
        String first = jwtService.parse(jwtService.generateRefreshToken(1L, Role.USER)).getId();
        String second = jwtService.parse(jwtService.generateRefreshToken(1L, Role.USER)).getId();

        assertNotEquals(first, second);
    }


    @Test
    void expiredToken_isRejected() throws Exception {
        // отрицательное время жизни: токен выдан уже просроченным
        JwtService expiring = newService("expired", -60_000L, -60_000L);
        String token = expiring.generateAccessToken(1L, Role.USER);

        assertThrows(InvalidTokenException.class, () -> expiring.parse(token));
    }

    @Test
    void tamperedToken_isRejected() {
        String token = jwtService.generateAccessToken(1L, Role.USER);
        String[] parts = token.split("\\.");
        char original = parts[1].charAt(10);
        char replacement = original == 'A' ? 'B' : 'A';
        parts[1] = parts[1].substring(0, 10) + replacement + parts[1].substring(11);
        String tampered = String.join(".", parts);

        assertThrows(InvalidTokenException.class, () -> jwtService.parse(tampered));
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() throws Exception {
        JwtService stranger = newService("stranger", ACCESS_TTL_MS, REFRESH_TTL_MS);
        String foreignToken = stranger.generateAccessToken(1L, Role.ADMIN);

        assertThrows(InvalidTokenException.class, () -> jwtService.parse(foreignToken));
    }

    @Test
    void garbage_isRejected() {
        assertThrows(InvalidTokenException.class, () -> jwtService.parse("not-a-token"));
    }

    @Test
    void emptyAndNullTokens_areRejected() {
        assertThrows(InvalidTokenException.class, () -> jwtService.parse(""));
        assertThrows(InvalidTokenException.class, () -> jwtService.parse(null));
    }
}