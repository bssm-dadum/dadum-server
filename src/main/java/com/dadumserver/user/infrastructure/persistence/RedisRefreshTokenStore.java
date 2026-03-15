package com.dadumserver.user.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
  private static final String KEY_PREFIX = "auth:refresh:";

  private final StringRedisTemplate redisTemplate;

  @Override
  public void save(UUID userId, String encodedRefreshToken, Duration ttl) {
    redisTemplate.opsForValue().set(buildKey(userId), encodedRefreshToken, ttl);
  }

  @Override
  public Optional<String> findByUserId(UUID userId) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(buildKey(userId)));
  }

  @Override
  public void delete(UUID userId) {
    redisTemplate.delete(buildKey(userId));
  }

  private String buildKey(UUID userId) {
    return KEY_PREFIX + userId;
  }
}
