package com.howtogrow.backend.infrastructure.ai;

import java.util.List;

public interface AiChatClient {
  String chat(List<ChatMessage> messages);

  record ChatMessage(String role, String content) {}
}

