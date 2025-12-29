package com.howtogrow.backend.infrastructure.pay;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import org.springframework.stereotype.Component;

@Component
public class WechatPayClientProvider {
  private final WechatPayProperties props;
  private final MockWechatPayClient mock;

  public WechatPayClientProvider(WechatPayProperties props, MockWechatPayClient mock) {
    this.props = props;
    this.mock = mock;
  }

  public WechatPayClient get() {
    if (props.mockEnabled()) {
      return mock;
    }
    throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay client not implemented");
  }
}

