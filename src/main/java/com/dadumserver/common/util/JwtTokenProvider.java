package com.dadumserver.common.util;

import com.dadumserver.common.config.JwtProperties;
import com.dadumserver.user.domain.model.Role;
import com.dadumserver.user.domain.model.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
  private static final String TOKEN_TYPE_CLAIM = "token_type";
  private static final String ROLE_CLAIM = "role";
  private static final String ACCESS_TOKEN_TYPE = "access";
  private static final String REFRESH_TOKEN_TYPE = "refresh";

  private final JwtProperties jwtProperties;
  private SecretKey secretKey;

  @PostConstruct
  public void initialize() {
    String secret = jwtProperties.getSecret();
    if (secret == null || secret.isBlank()) {
      throw new IllegalStateException("JWT secret must be configured");
    }

    byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalStateException("JWT secret must be at least 32 bytes");
    }

    this.secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateAccessToken(User user) {
    return generateToken(user, jwtProperties.getAccessTokenExpirationSeconds(), ACCESS_TOKEN_TYPE);
  }

  public String generateRefreshToken(User user) {
    return generateToken(user, jwtProperties.getRefreshTokenExpirationSeconds(), REFRESH_TOKEN_TYPE);
  }

  public boolean validateAccessToken(String token) {
    return validateTokenByType(token, ACCESS_TOKEN_TYPE);
  }

  public boolean validateRefreshToken(String token) {
    return validateTokenByType(token, REFRESH_TOKEN_TYPE);
  }

  public UUID getUserId(String token) {
    return UUID.fromString(parse(token).getPayload().getSubject());
  }

  public Role getUserRole(String token) {
    String role = parse(token).getPayload().get(ROLE_CLAIM, String.class);
    if (role == null) {
      return Role.user;
    }
    try {
      return Role.valueOf(role);
    } catch (IllegalArgumentException exception) {
      return Role.user;
    }
  }

  public long getAccessTokenExpirationSeconds() {
    return jwtProperties.getAccessTokenExpirationSeconds();
  }

  public long getRefreshTokenExpirationSeconds() {
    return jwtProperties.getRefreshTokenExpirationSeconds();
  }

  private String generateToken(User user, long expirationSeconds, String tokenType) {
    Instant now = Instant.now();
    Instant expiresAt = now.plusSeconds(expirationSeconds);

    return Jwts.builder()
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim(ROLE_CLAIM, user.getRole() == null ? Role.user.name() : user.getRole().name())
        .claim(TOKEN_TYPE_CLAIM, tokenType)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(secretKey)
        .compact();
  }

  private boolean validateTokenByType(String token, String expectedTokenType) {
    try {
      String tokenType = parse(token).getPayload().get(TOKEN_TYPE_CLAIM, String.class);
      return expectedTokenType.equals(tokenType);
    } catch (JwtException | IllegalArgumentException exception) {
      return false;
    }
  }

  private io.jsonwebtoken.Jws<io.jsonwebtoken.Claims> parse(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token);
  }
}
