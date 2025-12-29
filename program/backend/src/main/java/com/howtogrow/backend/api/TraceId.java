package com.howtogrow.backend.api;

import java.util.Objects;
import java.util.UUID;
import org.slf4j.MDC;

public final class TraceId {
  public static final String HEADER = "X-Trace-Id";
  public static final String MDC_KEY = "traceId";

  private TraceId() {}

  public static String current() {
    var traceId = MDC.get(MDC_KEY);
    if (traceId == null || traceId.isBlank()) {
      return "trace-" + UUID.randomUUID();
    }
    return traceId;
  }

  public static String sanitize(String traceId) {
    if (traceId == null) {
      return "";
    }
    var trimmed = traceId.trim();
    if (trimmed.length() > 128) {
      return trimmed.substring(0, 128);
    }
    return trimmed;
  }

  public static String firstNonBlank(String a, String b) {
    if (a != null && !a.isBlank()) {
      return a;
    }
    return Objects.requireNonNullElse(b, "");
  }
}

