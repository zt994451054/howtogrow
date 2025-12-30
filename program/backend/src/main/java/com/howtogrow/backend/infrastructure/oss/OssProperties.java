package com.howtogrow.backend.infrastructure.oss;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.oss")
public record OssProperties(
    boolean enabled,
    String endpoint,
    String accessKeyId,
    String accessKeySecret,
    String bucket,
    String publicBaseUrl,
    String avatarPrefix) {}

