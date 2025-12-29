package com.howtogrow.backend.infrastructure.ai;

import org.springframework.stereotype.Component;

@Component
public class AiChatClientProvider {
  private final AiProperties props;
  private final MockAiChatClient mock;
  private final HttpAiChatClient http;

  public AiChatClientProvider(AiProperties props, MockAiChatClient mock, HttpAiChatClient http) {
    this.props = props;
    this.mock = mock;
    this.http = http;
  }

  public AiChatClient get() {
    return props.mockEnabled() ? mock : http;
  }
}

