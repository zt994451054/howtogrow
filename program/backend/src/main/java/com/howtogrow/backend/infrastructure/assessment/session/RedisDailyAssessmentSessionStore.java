package com.howtogrow.backend.infrastructure.assessment.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.howtogrow.backend.config.DailyAssessmentProperties;
import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisDailyAssessmentSessionStore implements DailyAssessmentSessionStore {
  private static final long DEFAULT_TTL_SECONDS = 7200;
  private static final String KEY_PREFIX = "daily_assessment_session";

  private final StringRedisTemplate redis;
  private final ObjectMapper objectMapper;
  private final DailyAssessmentProperties properties;

  public RedisDailyAssessmentSessionStore(
      StringRedisTemplate redis, ObjectMapper objectMapper, DailyAssessmentProperties properties) {
    this.redis = redis;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @Override
  public void save(DailyAssessmentSession session, String sessionId) {
    var ttlSeconds = properties.sessionTtlSeconds() > 0 ? properties.sessionTtlSeconds() : DEFAULT_TTL_SECONDS;
    String key = key(session.userId(), session.childId(), sessionId);
    try {
      var json = objectMapper.writeValueAsString(session);
      redis.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize session", e);
    }
  }

  @Override
  public Optional<DailyAssessmentSession> find(long userId, long childId, String sessionId) {
    String key = key(userId, childId, sessionId);
    String json = redis.opsForValue().get(key);
    if (json == null || json.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(json, DailyAssessmentSession.class));
    } catch (JsonProcessingException e) {
      return Optional.empty();
    }
  }

  @Override
  public void delete(long userId, long childId, String sessionId) {
    redis.delete(key(userId, childId, sessionId));
  }

  private static String key(long userId, long childId, String sessionId) {
    return KEY_PREFIX + ":" + userId + ":" + childId + ":" + sessionId;
  }
}

