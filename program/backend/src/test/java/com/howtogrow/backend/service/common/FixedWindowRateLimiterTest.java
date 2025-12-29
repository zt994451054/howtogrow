package com.howtogrow.backend.service.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.howtogrow.backend.api.exception.AppException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FixedWindowRateLimiterTest {
  @Test
  void blocksAfterLimitInWindow() {
    var clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    var limiter = new FixedWindowRateLimiter(clock);

    assertDoesNotThrow(() -> limiter.require("k", Duration.ofMinutes(1), 2));
    assertDoesNotThrow(() -> limiter.require("k", Duration.ofMinutes(1), 2));
    assertThrows(AppException.class, () -> limiter.require("k", Duration.ofMinutes(1), 2));
  }
}

