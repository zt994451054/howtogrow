package com.howtogrow.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin.security")
public record AdminSecurityProperties(boolean enforcePermissionChecks) {}

