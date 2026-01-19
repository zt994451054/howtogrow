package com.howtogrow.backend.domain.quote;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum QuoteScene {
  DAILY_AWARENESS("每日觉察"),
  PARENTING_STATUS("育儿状态"),
  TROUBLE_ARCHIVE("烦恼档案"),
  PARENTING_DIARY("育儿日记");

  private static final Map<String, QuoteScene> BY_VALUE =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  v -> v.value.toLowerCase(Locale.ROOT), Function.identity()));

  private final String value;

  QuoteScene(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static List<String> allowedValues() {
    return Arrays.stream(values()).map(QuoteScene::value).toList();
  }

  public static Optional<QuoteScene> fromValue(String value) {
    if (value == null) {
      return Optional.empty();
    }
    var trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_VALUE.get(trimmed.toLowerCase(Locale.ROOT)));
  }
}

