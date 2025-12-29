package com.howtogrow.backend.infrastructure.wechat;

import org.springframework.stereotype.Component;

@Component
public class WechatClientProvider {
  private final WechatProperties props;
  private final MockWechatClient mockClient;
  private final HttpWechatClient httpClient;

  public WechatClientProvider(WechatProperties props, MockWechatClient mockClient, HttpWechatClient httpClient) {
    this.props = props;
    this.mockClient = mockClient;
    this.httpClient = httpClient;
  }

  public WechatClient get() {
    return props.mockEnabled() ? mockClient : httpClient;
  }
}
