package com.howtogrow.backend.domain.parenting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ParentingStatus {
  DISAPPOINTED("失望", "disappointed"),
  CALM("平静", "calm"),
  OPTIMISTIC("乐观", "optimistic"),
  SAD("难过", "sad"),
  HELPLESS("无奈", "helpless"),
  ANGRY("愤怒", "angry"),
  GRATIFIED("欣慰", "relieved"),
  WORRIED("担忧", "worried"),
  HAPPY("开心", "happy"),
  DESPERATE("绝望", "desperate");

  private static final Map<String, ParentingStatus> BY_VALUE =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  v -> v.value.toLowerCase(Locale.ROOT), Function.identity()));

  private final String value;
  private final String moodId;

  ParentingStatus(String value, String moodId) {
    this.value = value;
    this.moodId = moodId;
  }

  public String value() {
    return value;
  }

  public String moodId() {
    return moodId;
  }

  public static List<String> allowedValues() {
    return Arrays.stream(values()).map(ParentingStatus::value).toList();
  }

  public static Optional<ParentingStatus> fromValue(String value) {
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
