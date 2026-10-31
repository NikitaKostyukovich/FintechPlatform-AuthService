package com.innowise.authservice.integration;

import com.innowise.authservice.dto.request.LoginRequestDto;
import com.innowise.authservice.dto.response.LoginResponseDto;
import com.innowise.authservice.dto.request.RegisterRequestDto;
import com.innowise.authservice.entity.Credential;
import com.innowise.authservice.repository.CredentialRepository;
import com.innowise.authservice.repository.RefreshTokenRepository;
import com.innowise.authservice.role.Role;
import com.innowise.authservice.service.AuthService;
import com.innowise.authservice.service.JwtService;
import com.innowise.authservice.support.FakeUserService;
import com.innowise.authservice.support.TestKeys;
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