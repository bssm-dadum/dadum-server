package com.dadumserver.user.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "black_lists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlackList {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "blacklist_id", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "user_email_hashed", nullable = false)
  private String userEmailHashed;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public BlackList(String userEmailHashed, Instant createdAt) {
    this.userEmailHashed = userEmailHashed;
    this.createdAt = createdAt;
  }
}
