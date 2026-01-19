package com.howtogrow.backend.service.miniprogram;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MiniprogramNicknameGeneratorTest {
  @Test
  void randomNickname_isChineseAndNonBlank() {
    for (int i = 0; i < 200; i++) {
      var name = MiniprogramNicknameGenerator.randomNickname();
      assertFalse(name.isBlank());
      assertTrue(name.matches("^[\\u4e00-\\u9fff]+$"));
      assertTrue(name.length() >= 2 && name.length() <= 8);
    }
  }
}

