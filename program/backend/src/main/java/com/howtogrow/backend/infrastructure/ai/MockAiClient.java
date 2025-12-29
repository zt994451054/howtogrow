package com.howtogrow.backend.infrastructure.ai;

import org.springframework.stereotype.Component;

@Component
public class MockAiClient implements AiClient {
  @Override
  public AiTextResponse generateShortSummary(String prompt) {
    var content = "你已经很努力了，今天从一个小行动开始就很棒。";
    return new AiTextResponse(content, "mock", null);
  }
}

