package com.howtogrow.backend.service.common;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class FixedWindowRateLimiter {
  private final Clock clock;
  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  public FixedWindowRateLimiter(Clock clock) {
    this.clock = clock;
  }

  public void require(String key, Duration windowSize, int limit) {
    if (limit <= 0) {
      return;
    }
    var now = Instant.now(clock);
    windows.compute(
        key,
        (k, w) -> {
          if (w == null || now.isAfter(w.windowEnd)) {
            return new Window(now.plus(windowSize), 1);
          }
          if (w.count + 1 > limit) {
            throw new AppException(ErrorCode.RATE_LIMITED, "rate limited");
          }
          return new Window(w.windowEnd, w.count + 1);
        });
  }

  private record Window(Instant windowEnd, int count) {}
}

