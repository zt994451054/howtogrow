package com.howtogrow.backend.infrastructure.pay;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MockWechatPayClient implements WechatPayClient {
  @Override
  public PayParams createJsapiPayParams(String orderNo, int amountCent, String payerOpenid) {
    var nonce = UUID.randomUUID().toString().replace("-", "");
    return new PayParams(
        Long.toString(System.currentTimeMillis() / 1000),
        nonce,
        "prepay_id=mock_" + orderNo,
        "RSA",
        "mock_sign");
  }
}

