package com.innowise.authservice.service;

import com.innowise.authservice.client.UserServiceClient;
import com.innowise.authservice.dto.request.LoginRequestDto;
import com.innowise.authservice.dto.request.RegisterRequestDto;
import com.innowise.authservice.dto.response.AuthResponseDto;
import com.innowise.authservice.dto.response.LoginResponseDto;
import com.innowise.authservice.dto.response.ValidateResponseDto;
import com.innowise.authservice.entity.Credential;
import com.innowise.authservice.entity.RefreshToken;
import com.innowise.authservice.exception.InvalidCredentialsException;
import com.innowise.authservice.exception.InvalidTokenException;
import com.innowise.authservice.exception.UserAlreadyExistsException;
import com.innowise.authservice.exception.UserServiceException;
import com.innowise.authservice.mapper.CredentialMapper;
import com.innowise.authservice.repository.CredentialRepository;
import com.innowise.authservice.repository.RefreshTokenRepository;
import com.innowise.authservice.role.Role;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final CredentialRepository credentialRepository;
    private final UserServiceClient userServiceClient;
    private final CredentialMapper credentialMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(CredentialRepository credentialRepository, UserServiceClient userServiceClient, CredentialMapper credentialMapper, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenRepository refreshTokenRepository) {
        this.credentialRepository = credentialRepository;
        this.userServiceClient = userServiceClient;
        this.credentialMapper = credentialMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public Long register(RegisterRequestDto dto) {
        log.info("Attempting to register new user with login: {}", dto.login());

        if (credentialRepository.existsByLogin(dto.login())) {
            log.warn("Registration failed: User with login '{}' already exists", dto.login());
            throw new UserAlreadyExistsException("User with such login already exists");
        }

        String hashedPassword = passwordEncoder.encode(dto.password());

        log.debug("Calling UserServiceClient to create user profile for '{}'", dto.login());
        AuthResponseDto response = userServiceClient.createUser(dto);

        if (response == null || response.id() == null) {
            log.error("UserServiceClient returned empty response or null ID during registration of '{}'", dto.login());
            throw new UserServiceException("User service returned no user id");
        }

        Long userId = response.id();
        log.info("User profile created successfully in UserService with ID: {}", userId);

        try {
            Credential newCredential = credentialMapper.toEntity(dto, userId, hashedPassword);
            credentialRepository.save(newCredential);
            log.info("Credentials saved successfully for user ID: {}", userId);
            return userId;

        } catch (RuntimeException ex) {
            log.error("Error while saving credentials for user ID: {}, triggering rollback", userId, ex);
            rollbackUserProfile(userId);
            if (ex instanceof DataIntegrityViolationException) {
                log.warn("Data integrity violation: User with login '{}' already exists", dto.login());
                throw new UserAlreadyExistsException("User with such login already exists");
            }
            throw ex;
        }
    }

    public LoginResponseDto login(LoginRequestDto dto) {
        log.info("Login attempt for user: {}", dto.login());

        Credential credential = credentialRepository.findByLogin(dto.login())
                .orElseThrow(() -> {
                    log.warn("Login failed: User '{}' not found", dto.login());
                    return new InvalidCredentialsException("Invalid login or password");
                });

        if (!passwordEncoder.matches(dto.password(), credential.getPasswordHash())) {
            log.warn("Login failed: Invalid password for user '{}'", dto.login());
            throw new BadCredentialsException("Invalid login or password");
        }

        log.info("User '{}' (ID: {}) logged in successfully", dto.login(), credential.getUserId());
        return issueTokens(credential);
    }

    private void rollbackUserProfile(Long userId) {
        try {
            log.warn("Rolling back user profile for user ID: {}", userId);
            userServiceClient.deleteUser(userId);
            log.info("Successfully rolled back user profile for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Could not delete user {} in User Service after failed registration, manual cleanup required", userId, e);
        }
    }

    @Transactional
    public LoginResponseDto refresh(String refreshToken) {
        log.debug("Processing token refresh request");

        Claims claims = jwtService.parse(refreshToken);
        if (!JwtService.TYPE_REFRESH.equals(claims.get("token_type"))) {
            log.warn("Token refresh failed: Provided token is not a refresh token");
            throw new InvalidTokenException("Not a refresh token");
        }

        String jti = claims.getId();
        if (refreshTokenRepository.deleteByJti(jti) == 0) {
            log.warn("Token refresh failed: Refresh token (jti: {}) is not active or already used", jti);
            throw new InvalidTokenException("Refresh token is not active");
        }

        Long userId = ((Number) claims.get("user_id")).longValue();
        Credential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Token refresh failed: User with ID {} no longer exists in credentials table", userId);
                    return new InvalidTokenException("User no longer exists");
                });

        log.info("Tokens successfully refreshed for user ID: {}", userId);
        return issueTokens(credential);
    }

    public ValidateResponseDto validate(String token) {
        log.debug("Validating access token");
        try {
            Claims claims = jwtService.parse(token);
            if (!JwtService.TYPE_ACCESS.equals(claims.get("token_type", String.class))) {
                log.warn("Token validation failed: Provided token is not an access token");
                return new ValidateResponseDto(false, null, null);
            }

            Long userId = ((Number) claims.get("user_id")).longValue();
            Role role = Role.valueOf(claims.get("role", String.class));

            log.debug("Token validated successfully for user ID: {}", userId);
            return new ValidateResponseDto(true, userId, role);

        } catch (InvalidTokenException e) {
            log.warn("Token validation failed due to invalid token format or expiration: {}", e.getMessage());
            return new ValidateResponseDto(false, null, null);
        }
    }

    private LoginResponseDto issueTokens(Credential credential) {
        log.debug("Issuing new token pair for user ID: {}", credential.getUserId());

        String accessToken = jwtService.generateAccessToken(credential.getUserId(), credential.getRole());
        String refreshToken = jwtService.generateRefreshToken(credential.getUserId(), credential.getRole());

        Claims claims = jwtService.parse(refreshToken);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());

        refreshTokenRepository.save(new RefreshToken(claims.getId(), credential.getUserId(), expiresAt));
        log.debug("Refresh token (jti: {}) saved to database for user ID: {}", claims.getId(), credential.getUserId());

        return new LoginResponseDto(accessToken, refreshToken);
    }
}