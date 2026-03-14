package com.dadumserver.user.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name="user_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name="email", nullable = false, unique = true)
  private String email;

  @Column(name="password", nullable = false)
  private String password;

  @Column(name = "role")
  @Enumerated(EnumType.STRING)
  private Role role;

  public User(UUID id, String email, String password) {
    this(id, email, password, Role.user);
  }

  public User(UUID id, String email, String password, Role role) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.role = role;
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
