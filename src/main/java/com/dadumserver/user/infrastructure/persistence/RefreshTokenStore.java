package com.dadumserver.user.infrastructure.persistence;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenStore {
  void save(UUID userId, String encodedRefreshToken, Duration ttl);

  Optional<String> findByUserId(UUID userId);

  void delete(UUID userId);
}
