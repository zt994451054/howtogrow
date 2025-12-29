package com.howtogrow.backend.infrastructure.wechat;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wechat")
public record WechatProperties(String appid, String secret, boolean mockEnabled) {}

