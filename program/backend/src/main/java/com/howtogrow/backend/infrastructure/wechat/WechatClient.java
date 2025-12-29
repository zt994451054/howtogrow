package com.howtogrow.backend.infrastructure.wechat;

public interface WechatClient {
  WechatSession exchangeLoginCode(String code);
}

