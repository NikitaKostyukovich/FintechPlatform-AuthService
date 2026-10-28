package com.nikita.authservice.integration;

import com.nikita.authservice.entity.Credential;
import com.nikita.authservice.exception.UserAlreadyExistsException;
import com.nikita.authservice.exception.UserServiceException;
import com.nikita.authservice.role.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterIntegrationTest extends AbstractAuthIntegrationTest {

    @Test
    void success_createsProfileAndSavesCredentialWithUserRole() {
        Long userId = authService.register(registerDto("ivan", "secret123"));

        assertEquals(100L, userId);

        Credential saved = credentialRepository.findByLogin("ivan").orElseThrow();
        assertEquals(100L, saved.getUserId());
        assertEquals(Role.USER, saved.getRole());
        assertNotEquals("secret123", saved.getPasswordHash());
        assertTrue(saved.getPasswordHash().startsWith("$2"));

        assertEquals(1, FAKE_USER_SERVICE.createRequests().size());
        assertTrue(FAKE_USER_SERVICE.deletedIds().isEmpty());
    }

    @Test
    void existingLogin_isRejectedBeforeCallingUserService() {
        saveCredential(5L, "ivan", "secret123", Role.USER);

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerDto("ivan", "another-pass")));

        assertTrue(FAKE_USER_SERVICE.createRequests().isEmpty());
        assertEquals(1, credentialRepository.count());
    }

    @Test
    void userServiceError_failsWithoutSavingAnything() {
        FAKE_USER_SERVICE.respondToCreateWith(500, "{}");

        assertThrows(UserServiceException.class,
                () -> authService.register(registerDto("ivan", "secret123")));

        assertEquals(0, credentialRepository.count());
        assertTrue(FAKE_USER_SERVICE.deletedIds().isEmpty());
    }

    @Test
    void userServiceReturnsNoId_failsWithoutSavingAnything() {
        FAKE_USER_SERVICE.respondToCreateWith(201, "{}");

        assertThrows(UserServiceException.class,
                () -> authService.register(registerDto("ivan", "secret123")));

        assertEquals(0, credentialRepository.count());
    }

    @Test
    void credentialSaveFails_profileIsRolledBackInUserService() {

        saveCredential(100L, "other", "secret123", Role.USER);

        assertThrows(UserAlreadyExistsException.class,
                () -> authService.register(registerDto("ivan", "secret123")));

        assertEquals(1, FAKE_USER_SERVICE.createRequests().size());
        assertEquals(java.util.List.of(100L), FAKE_USER_SERVICE.deletedIds());
        assertEquals(1, credentialRepository.count());
    }
}