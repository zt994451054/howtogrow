package com.howtogrow.backend.infrastructure.ai;

public interface AiClient {
  AiTextResponse generateShortSummary(String prompt);
}

