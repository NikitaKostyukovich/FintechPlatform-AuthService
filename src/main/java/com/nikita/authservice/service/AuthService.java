package com.nikita.authservice.service;

import com.nikita.authservice.client.UserServiceClient;
import com.nikita.authservice.dto.*;
import com.nikita.authservice.entity.Credential;
import com.nikita.authservice.entity.RefreshToken;
import com.nikita.authservice.exception.InvalidCredentialsException;
import com.nikita.authservice.exception.InvalidTokenException;
import com.nikita.authservice.exception.UserAlreadyExistsException;
import com.nikita.authservice.exception.UserServiceException;
import com.nikita.authservice.mapper.CredentialMapper;
import com.nikita.authservice.repository.CredentialRepository;
import com.nikita.authservice.repository.RefreshTokenRepository;
import com.nikita.authservice.role.Role;
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

    public AuthService(CredentialRepository credentialRepository, UserServiceClient userServiceClient, CredentialMapper credentialMapper, PasswordEncoder passwordEncoder, JwtService jwtService,RefreshTokenRepository refreshTokenRepository) {
        this.credentialRepository = credentialRepository;
        this.userServiceClient = userServiceClient;
        this.credentialMapper = credentialMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public Long register(RegisterRequestDto dto) {

        if (credentialRepository.existsByLogin(dto.login())) {
            throw new UserAlreadyExistsException("User with such login already exists");
        }
        String hashedPassword = passwordEncoder.encode(dto.password());
        AuthResponseDto response = userServiceClient.createUser(dto);
        if (response == null || response.id() == null) {
            throw new UserServiceException(
                    "User service returned no user id"
            );
        }
        Long userId = response.id();
        try {
            Credential newCredential = credentialMapper.toEntity(dto, userId, hashedPassword);
            credentialRepository.save(newCredential);
            return userId;

        } catch (RuntimeException ex) {
            rollbackUserProfile(userId);
            if (ex instanceof DataIntegrityViolationException) {
                throw new UserAlreadyExistsException("User with such login already exists");
            }
            throw ex;
        }

    }
    public LoginResponseDto login(LoginRequestDto dto) {
        Credential credential = credentialRepository.findByLogin(dto.login())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid login or password"));

        if (!passwordEncoder.matches(dto.password(), credential.getPasswordHash())) {
            throw new BadCredentialsException("Invalid login or password");
        }

        return issueTokens(credential);
    }


private void rollbackUserProfile(Long userId) {
    try {
        userServiceClient.deleteUser(userId);
    } catch (Exception e) {
        log.error("Could not delete user {} in User Service after failed registration, manual cleanup required",
                userId, e);
    }
}
@Transactional
public LoginResponseDto refresh(String refreshToken){
    Claims claims = jwtService.parse(refreshToken);
    if(!JwtService.TYPE_REFRESH.equals(claims.get("token_type"))){
        throw new InvalidTokenException("Not a refresh token");
    }
    if (refreshTokenRepository.deleteByJti(claims.getId())==0) {
        throw new InvalidTokenException("Refresh token is not active");
    }

    Long userId = ((Number) claims.get("user_id")).longValue();
    Credential credential = credentialRepository.findByUserId(userId)
            .orElseThrow(() -> new InvalidTokenException("User no longer exists"));
    return issueTokens(credential);
}

public ValidateResponseDto validate(String token){
    try{
        Claims claims = jwtService.parse(token);
        if(!JwtService.TYPE_ACCESS.equals(claims.get("token_type", String.class))){
            return new ValidateResponseDto(false, null, null);
        }
        Long userId =((Number) claims.get("user_id")).longValue();
        Role role = Role.valueOf(claims.get("role", String.class)) ;
        return new ValidateResponseDto(true, userId, role);
    }catch (InvalidTokenException e){
        return new ValidateResponseDto(false, null, null);
    }
}

private LoginResponseDto issueTokens(Credential credential){
    String accessToken = jwtService.generateAccessToken(credential.getUserId(), credential.getRole());
    String refreshToken = jwtService.generateRefreshToken(credential.getUserId(), credential.getRole());
    Claims claims = jwtService.parse(refreshToken);
    LocalDateTime expiresAt = LocalDateTime.ofInstant(claims.getExpiration().toInstant(), ZoneId.systemDefault());
    refreshTokenRepository.save(new RefreshToken(claims.getId(), credential.getUserId(), expiresAt));
    return new LoginResponseDto(accessToken, refreshToken);

}
}
