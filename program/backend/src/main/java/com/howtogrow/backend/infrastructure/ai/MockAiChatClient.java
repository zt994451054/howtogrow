package com.howtogrow.backend.infrastructure.ai;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MockAiChatClient implements AiChatClient {
  @Override
  public String chat(List<ChatMessage> messages) {
    return "我听到了。我们可以先从一个很小、可执行的步骤开始：今天只练习一句温和的肯定。";
  }
}

