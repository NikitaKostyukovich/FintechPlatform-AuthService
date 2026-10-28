package com.nikita.authservice.service;

import com.nikita.authservice.exception.InvalidTokenException;
import com.nikita.authservice.role.Role;
import com.nimbusds.jose.JWSAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigInteger;
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

        this.privateKey = loadPrivateKey(privateKeyPath);
        this.publicKey = loadPublicKey(publicKeyPath);
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    private PrivateKey loadPrivateKey(String path)
            throws IOException, GeneralSecurityException {

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

        String pem = Files.readString(Path.of(path))
                .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                .replaceAll("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(pem);

        return KeyFactory.getInstance("RSA")
                .generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    public String generateAccessToken(Long userId, Role role) {
        return generateToken(
                userId, role, "access", accessTokenExpiration
        );
    }

    public String generateRefreshToken(Long userId, Role role) {
        return generateToken(
                userId, role, "refresh", refreshTokenExpiration
        );
    }

    private String generateToken(
            Long userId,
            Role role,
            String tokenType,
            long expiration
    ) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .header().keyId("auth-service-key").and()
                .id(UUID.randomUUID().toString())
                .claim("user_id", userId)
                .claim("role", role.name())
                .claim("token_type", tokenType)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("Invalid or expired token");
        }
    }
    public Map<String, Object> getJwks(){
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) publicKey)
                .keyID("auth-service-key")
                .algorithm(JWSAlgorithm.RS256)
                .build();
        return new JWKSet(rsaKey).toJSONObject();
    }
}