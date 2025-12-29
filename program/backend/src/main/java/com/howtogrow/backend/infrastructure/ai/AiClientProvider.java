package com.howtogrow.backend.infrastructure.ai;

import org.springframework.stereotype.Component;

@Component
public class AiClientProvider {
  private final AiProperties props;
  private final MockAiClient mock;
  private final HttpAiClient http;

  public AiClientProvider(AiProperties props, MockAiClient mock, HttpAiClient http) {
    this.props = props;
    this.mock = mock;
    this.http = http;
  }

  public AiClient get() {
    return props.mockEnabled() ? mock : http;
  }
}

