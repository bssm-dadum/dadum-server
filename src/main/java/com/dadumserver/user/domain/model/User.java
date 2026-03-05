package com.dadumserver.user.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name="user_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name="email", nullable = false)
  private String email;

  @Column(name="password", nullable = false)
  private String password;

  @Column(name = "refresh_token", length = 255)
  private String refreshToken;

  @Column(name = "refresh_token_expires_at")
  private Instant refreshTokenExpiresAt;

  public User(UUID id, String email, String password) {
    this.id = id;
    this.email = email;
    this.password = password;
  }

  public void updateRefreshToken(String refreshToken, Instant refreshTokenExpiresAt) {
    this.refreshToken = refreshToken;
    this.refreshTokenExpiresAt = refreshTokenExpiresAt;
  }

  public void clearRefreshToken() {
    this.refreshToken = null;
    this.refreshTokenExpiresAt = null;
  }

  public void updateProfile(String email, String password) {
    if (email != null) {
      this.email = email;
    }
    if (password != null) {
      this.password = password;
    }
  }
}
