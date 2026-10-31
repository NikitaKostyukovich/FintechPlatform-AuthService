package com.innowise.authservice.service;

import com.innowise.authservice.exception.InvalidTokenException;
import com.innowise.authservice.role.Role;
import com.nimbusds.jose.JWSAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.security.interfaces.RSAPublicKey;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtService(
            @Value("${jwt.private-key-path}") String privateKeyPath,
            @Value("${jwt.public-key-path}") String publicKeyPath,
            @Value("${jwt.access-token.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token.expiration}") long refreshTokenExpiration
    ) throws IOException, GeneralSecurityException {

        log.info("Initializing JwtService, loading RSA keys...");

        this.privateKey = loadPrivateKey(privateKeyPath);
        this.publicKey = loadPublicKey(publicKeyPath);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;

        log.info("RSA keys loaded successfully. Access token TTL: {}ms, Refresh token TTL: {}ms",
                accessTokenExpiration, refreshTokenExpiration);
    }

    private PrivateKey loadPrivateKey(String path)
            throws IOException, GeneralSecurityException {

        log.debug("Loading private key from path: {}", path);
        String pem = Files.readString(Path.of(path))
                .replaceAll("-----BEGIN PRIVATE KEY-----", "")
                .replaceAll("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(pem);

        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private PublicKey loadPublicKey(String path)
            throws IOException, GeneralSecurityException {

        log.debug("Loading public key from path: {}", path);
        String pem = Files.readString(Path.of(path))
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(pem);

        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    public String generateAccessToken(Long userId, Role role) {
        return generateToken(userId, role, TYPE_ACCESS, accessTokenExpiration);
    }

    public String generateRefreshToken(Long userId, Role role) {
        return generateToken(userId, role, TYPE_REFRESH, refreshTokenExpiration);
    }

    private String generateToken(
            Long userId,
            Role role,
            String tokenType,
            long expiration
    ) {
        log.debug("Generating {} token for user ID: {}", tokenType, userId);
        long now = System.currentTimeMillis();
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .header().keyId("auth-service-key").and()
                .id(jti)
                .claim("user_id", userId)
                .claim("role", role.name())
                .claim("token_type", tokenType)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();

        log.debug("Successfully generated {} token (jti: {}) for user ID: {}", tokenType, jti, userId);
        return token;
    }

    public Claims parse(String token) {
        log.debug("Attempting to parse and verify JWT token");
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("JWT token (jti: {}) parsed and verified successfully", claims.getId());
            return claims;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Failed to parse JWT token (invalid signature, expired, or malformed): {}", e.getMessage());
            throw new InvalidTokenException("Invalid or expired token");
        }
    }

    public Map<String, Object> getJwks() {
        log.debug("Generating JWKS (JSON Web Key Set) for external services");
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                .keyID("auth-service-key")
                .algorithm(JWSAlgorithm.RS256)
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}