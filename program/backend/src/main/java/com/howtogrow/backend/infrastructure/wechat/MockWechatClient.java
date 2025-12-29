package com.howtogrow.backend.infrastructure.wechat;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import org.springframework.stereotype.Component;

@Component
public class MockWechatClient implements WechatClient {
  @Override
  public WechatSession exchangeLoginCode(String code) {
    if (code == null || code.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "code is required");
    }
    var openid = code.startsWith("mock:") ? code.substring("mock:".length()) : "mock_" + code;
    if (openid.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "invalid mock code");
    }
    return new WechatSession(openid, null);
  }
}

