package com.howtogrow.backend.service.miniprogram;

final class AiChatTitleNormalizer {
  private static final int TITLE_MAX_CODE_POINTS = 128;

  private AiChatTitleNormalizer() {}

  static String normalizeForTitle(String userContent) {
    if (userContent == null) {
      return null;
    }
    var trimmed = userContent.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    var normalized = trimmed.replaceAll("\\s+", " ").trim();
    if (normalized.isEmpty()) {
      return null;
    }
    return truncateByCodePoints(normalized, TITLE_MAX_CODE_POINTS);
  }

  private static String truncateByCodePoints(String input, int maxCodePoints) {
    if (input == null || input.isEmpty()) {
      return input;
    }
    if (maxCodePoints <= 0) {
      return "";
    }
    var total = input.codePointCount(0, input.length());
    if (total <= maxCodePoints) {
      return input;
    }
    var endIndex = input.offsetByCodePoints(0, maxCodePoints);
    return input.substring(0, endIndex);
  }
}

