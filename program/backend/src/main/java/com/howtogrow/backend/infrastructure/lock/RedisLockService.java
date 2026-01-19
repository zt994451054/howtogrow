package com.howtogrow.backend.infrastructure.lock;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisLockService {
  private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
      new DefaultRedisScript<>(
          """
          if redis.call('get', KEYS[1]) == ARGV[1] then
            return redis.call('del', KEYS[1])
          else
            return 0
          end
          """,
          Long.class);

  private final StringRedisTemplate redis;

  public RedisLockService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public Optional<String> tryLock(String key, Duration ttl) {
    if (key == null || key.isBlank()) return Optional.empty();
    Duration effectiveTtl = ttl != null && !ttl.isNegative() && !ttl.isZero() ? ttl : Duration.ofSeconds(5);
    String token = UUID.randomUUID().toString();
    Boolean ok = redis.opsForValue().setIfAbsent(key, token, effectiveTtl);
    return Boolean.TRUE.equals(ok) ? Optional.of(token) : Optional.empty();
  }

  public void unlock(String key, String token) {
    if (key == null || key.isBlank()) return;
    if (token == null || token.isBlank()) return;
    redis.execute(RELEASE_SCRIPT, List.of(key), token);
  }
}

