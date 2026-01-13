package com.howtogrow.backend.infrastructure.pay;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.wechat-pay")
public record WechatPayProperties(
    String mchId,
    String mchSerialNo,
    String apiV3Key,
    String privateKeyPath,
    String notifyUrl) {}
