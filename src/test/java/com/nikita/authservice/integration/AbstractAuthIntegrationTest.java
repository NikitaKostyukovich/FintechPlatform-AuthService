package com.nikita.authservice.integration;

import com.nikita.authservice.dto.LoginRequestDto;
import com.nikita.authservice.dto.LoginResponseDto;
import com.nikita.authservice.dto.RegisterRequestDto;
import com.nikita.authservice.entity.Credential;
import com.nikita.authservice.repository.CredentialRepository;
import com.nikita.authservice.repository.RefreshTokenRepository;
import com.nikita.authservice.role.Role;
import com.nikita.authservice.service.AuthService;
import com.nikita.authservice.service.JwtService;
import com.nikita.authservice.support.FakeUserService;
import com.nikita.authservice.support.TestKeys;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;


@SpringBootTest
public abstract class AbstractAuthIntegrationTest {

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
    static final FakeUserService FAKE_USER_SERVICE = new FakeUserService();
    static final TestKeys.KeyFiles KEYS;

    static {
        POSTGRES.start();
        try {
            KEYS = TestKeys.writeKeyFiles(Files.createTempDirectory("auth-test-keys"));
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot prepare test keys", e);
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("jwt.private-key-path", () -> KEYS.privateKey().toString());
        registry.add("jwt.public-key-path", () -> KEYS.publicKey().toString());
        registry.add("jwt.access-token.expiration", () -> "900000");
        registry.add("jwt.refresh-token.expiration", () -> "604800000");
        registry.add("application.client.user-service.url", FAKE_USER_SERVICE::baseUrl);
    }

    @Autowired
    protected AuthService authService;
    @Autowired
    protected JwtService jwtService;
    @Autowired
    protected CredentialRepository credentialRepository;
    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        credentialRepository.deleteAll();
        FAKE_USER_SERVICE.reset();
    }

    protected void saveCredential(long userId, String login, String rawPassword, Role role) {
        Credential credential = new Credential();
        credential.setUserId(userId);
        credential.setLogin(login);
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setRole(role);
        credentialRepository.save(credential);
    }

    protected LoginResponseDto loginAsIvan() {
        saveCredential(1L, "ivan", "secret123", Role.USER);
        return authService.login(new LoginRequestDto("ivan", "secret123"));
    }


    protected RegisterRequestDto registerDto(String login, String password) {
        return new RegisterRequestDto(login, password, login + "@test.com", "Ivan",
        "Petrov", LocalDate.of(1999, 5, 20));
    }
}