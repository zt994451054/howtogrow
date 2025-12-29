package com.howtogrow.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.howtogrow.backend.api.exception.AppException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
  @Test
  void issuesAndVerifiesMiniprogramToken() {
    var props =
        new JwtProperties(
            "01234567890123456789012345678901",
            "abcdefabcdefabcdefabcdefabcdefab",
            60);
    var clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    var service = new JwtService(props, clock);

    var token = service.issue(Audience.MINIPROGRAM, 123L);
    var user = service.verify(token);
    assertEquals(123L, user.userId());
    assertEquals(Audience.MINIPROGRAM, user.audience());
  }

  @Test
  void rejectsExpiredToken() {
    var props =
        new JwtProperties(
            "01234567890123456789012345678901",
            "abcdefabcdefabcdefabcdefabcdefab",
            1);
    var baseClock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
    var service = new JwtService(props, baseClock);
    var token = service.issue(Audience.ADMIN, 1L);

    var expiredClock = Clock.offset(baseClock, Duration.ofSeconds(5));
    var verifyService = new JwtService(props, expiredClock);
    assertThrows(AppException.class, () -> verifyService.verify(token));
  }
}

