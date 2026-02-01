package com.howtogrow.backend.domain.child;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ParentIdentity {
  FATHER("爸爸"),
  MOTHER("妈妈"),
  PATERNAL_GRANDMA("奶奶"),
  PATERNAL_GRANDPA("爷爷"),
  MATERNAL_GRANDPA("外公"),
  MATERNAL_GRANDMA("外婆"),
  OTHER_GUARDIAN("其他监护人");

  private static final Map<String, ParentIdentity> BY_VALUE =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  v -> v.value.toLowerCase(Locale.ROOT), Function.identity()));

  private final String value;

  ParentIdentity(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static List<String> allowedValues() {
    return Arrays.stream(values()).map(ParentIdentity::value).toList();
  }

  public static Optional<ParentIdentity> fromValue(String value) {
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
