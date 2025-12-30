package com.howtogrow.backend.infrastructure.pay;

import org.springframework.stereotype.Component;

@Component
public class WechatPayClientProvider {
  private final WechatPayProperties props;
  private final MockWechatPayClient mock;
  private final WxJavaWechatPayClient wxJavaClient;

  public WechatPayClientProvider(
      WechatPayProperties props, MockWechatPayClient mock, WxJavaWechatPayClient wxJavaClient) {
    this.props = props;
    this.mock = mock;
    this.wxJavaClient = wxJavaClient;
  }

  public WechatPayClient get() {
    if (props.mockEnabled()) {
      return mock;
    }
    return wxJavaClient;
  }
}
