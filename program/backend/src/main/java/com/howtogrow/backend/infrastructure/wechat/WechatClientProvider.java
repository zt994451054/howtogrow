package com.howtogrow.backend.infrastructure.wechat;

import org.springframework.stereotype.Component;

@Component
public class WechatClientProvider {
  private final WechatProperties props;
  private final MockWechatClient mockClient;
  private final WxJavaWechatClient wxJavaClient;

  public WechatClientProvider(WechatProperties props, MockWechatClient mockClient, WxJavaWechatClient wxJavaClient) {
    this.props = props;
    this.mockClient = mockClient;
    this.wxJavaClient = wxJavaClient;
  }

  public WechatClient get() {
    return props.mockEnabled() ? mockClient : wxJavaClient;
  }
}
