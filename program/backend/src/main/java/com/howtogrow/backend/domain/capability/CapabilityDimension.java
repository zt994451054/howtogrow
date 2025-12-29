package com.howtogrow.backend.domain.capability;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum CapabilityDimension {
  EMOTION_MANAGEMENT("EMOTION_MANAGEMENT", "情绪管理力", 1),
  COMMUNICATION_EXPRESSION("COMMUNICATION_EXPRESSION", "沟通表达力", 2),
  RULE_GUIDANCE("RULE_GUIDANCE", "规则引导力", 3),
  RELATIONSHIP_BUILDING("RELATIONSHIP_BUILDING", "关系建设力", 4),
  LEARNING_SUPPORT("LEARNING_SUPPORT", "学习支持力", 5);

  private static final Map<String, CapabilityDimension> BY_CODE =
      Arrays.stream(values())
          .collect(
              Collectors.toUnmodifiableMap(
                  d -> d.code.toUpperCase(Locale.ROOT), Function.identity()));

  private final String code;
  private final String displayName;
  private final int sortNo;

  CapabilityDimension(String code, String displayName, int sortNo) {
    this.code = code;
    this.displayName = displayName;
    this.sortNo = sortNo;
  }

  public String code() {
    return code;
  }

  public String displayName() {
    return displayName;
  }

  public int sortNo() {
    return sortNo;
  }

  public static Optional<CapabilityDimension> fromCode(String code) {
    if (code == null) {
      return Optional.empty();
    }
    var normalized = code.trim();
    if (normalized.isEmpty()) {
      return Optional.empty();
    }
    return Optional.ofNullable(BY_CODE.get(normalized.toUpperCase(Locale.ROOT)));
  }

  public static String displayNameOf(String code) {
    return fromCode(code).map(CapabilityDimension::displayName).orElse(code);
  }

  public static int sortNoOf(String code) {
    return fromCode(code).map(CapabilityDimension::sortNo).orElse(Integer.MAX_VALUE);
  }

  public static List<CapabilityDimension> ordered() {
    return Arrays.stream(values()).sorted(Comparator.comparingInt(CapabilityDimension::sortNo)).toList();
  }
}
