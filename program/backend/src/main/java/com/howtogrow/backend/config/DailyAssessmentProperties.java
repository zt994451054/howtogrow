package com.howtogrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.daily-assessment")
public record DailyAssessmentProperties(long sessionTtlSeconds) {}

