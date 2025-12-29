package com.howtogrow.backend.infrastructure.pay;

public interface WechatPayClient {
  PayParams createJsapiPayParams(String orderNo, int amountCent, String payerOpenid);

  record PayParams(String timeStamp, String nonceStr, String packageValue, String signType, String paySign) {}
}

