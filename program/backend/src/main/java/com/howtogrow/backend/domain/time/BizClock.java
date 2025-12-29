package com.howtogrow.backend.domain.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class BizClock {
  private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
  private final Clock clock;

  public BizClock(Clock clock) {
    this.clock = clock;
  }

  public LocalDate today() {
    return LocalDate.now(clock.withZone(BIZ_ZONE));
  }
}

