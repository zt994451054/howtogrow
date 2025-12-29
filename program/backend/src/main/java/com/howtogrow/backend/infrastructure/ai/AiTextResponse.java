package com.howtogrow.backend.infrastructure.ai;

public record AiTextResponse(String content, String modelName, Integer tokenUsage) {}

