package com.howtogrow.backend.service.miniprogram;

import java.security.SecureRandom;

final class MiniprogramNicknameGenerator {
  private static final SecureRandom RNG = new SecureRandom();

  private static final String[] PREFIXES = {
    "小小",
    "暖心",
    "认真",
    "温柔",
    "勇敢",
    "开心",
    "明亮",
    "元气",
    "治愈",
    "踏实",
  };

  private static final String[] NOUNS = {
    "树苗",
    "星星",
    "彩虹",
    "向日葵",
    "小熊",
    "鲸鱼",
    "小鹿",
    "小猫",
    "小兔",
    "小象",
  };

  private static final String[] SUFFIXES = {
    "",
    "一",
    "二",
    "三",
    "四",
    "五",
    "六",
    "七",
    "八",
    "九",
  };

  private MiniprogramNicknameGenerator() {}

  static String randomNickname() {
    return randomNickname(RNG);
  }

  static String randomNickname(SecureRandom rng) {
    var prefix = pick(rng, PREFIXES);
    var noun = pick(rng, NOUNS);
    var suffix = pick(rng, SUFFIXES);
    return limitCodePoints(prefix + noun + suffix, 8);
  }

  private static String pick(SecureRandom rng, String[] arr) {
    if (arr == null || arr.length == 0) return "";
    var idx = Math.floorMod(rng.nextInt(), arr.length);
    return arr[idx] == null ? "" : arr[idx];
  }

  private static String limitCodePoints(String s, int max) {
    if (s == null) return "";
    if (max <= 0) return "";
    var count = s.codePointCount(0, s.length());
    if (count <= max) return s;
    var end = s.offsetByCodePoints(0, max);
    return s.substring(0, end);
  }
}

