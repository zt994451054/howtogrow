package com.howtogrow.backend.service.miniprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class AiChatTitleNormalizerTest {
  @Test
  void normalizeForTitle_nullAndBlank_returnsNull() {
    assertNull(AiChatTitleNormalizer.normalizeForTitle(null));
    assertNull(AiChatTitleNormalizer.normalizeForTitle(""));
    assertNull(AiChatTitleNormalizer.normalizeForTitle("   \n\t  "));
  }

  @Test
  void normalizeForTitle_collapsesWhitespace() {
    assertEquals("a b c", AiChatTitleNormalizer.normalizeForTitle("  a \n b\tc  "));
  }

  @Test
  void normalizeForTitle_truncatesByCodePoints() {
    var longAscii = "a".repeat(200);
    var normalizedAscii = AiChatTitleNormalizer.normalizeForTitle(longAscii);
    assertEquals(128, normalizedAscii.codePointCount(0, normalizedAscii.length()));

    var longEmoji = "😀".repeat(200);
    var normalizedEmoji = AiChatTitleNormalizer.normalizeForTitle(longEmoji);
    assertEquals(128, normalizedEmoji.codePointCount(0, normalizedEmoji.length()));
  }
}

