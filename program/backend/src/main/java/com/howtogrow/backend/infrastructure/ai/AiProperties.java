package com.howtogrow.backend.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    String baseUrl,
    String apiKey,
    String model,
    String chatCompletionsPath,
    boolean mockEnabled) {}
