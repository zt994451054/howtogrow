package com.howtogrow.backend.infrastructure.wechat;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Component;

@Component
public class WxJavaWechatClient implements WechatClient {
  private final WechatProperties props;

  public WxJavaWechatClient(WechatProperties props) {
    this.props = props;
  }

  @Override
  public WechatSession exchangeLoginCode(String code) {
    if (code == null || code.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "code is required");
    }
    if (props.appid() == null
        || props.appid().isBlank()
        || props.secret() == null
        || props.secret().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat app config missing");
    }

    try {
      var service = createService(props.appid(), props.secret());
      var session = service.getUserService().getSessionInfo(code);
      if (session == null || session.getOpenid() == null || session.getOpenid().isBlank()) {
        throw new AppException(ErrorCode.UNAUTHORIZED, "wechat login failed");
      }
      return new WechatSession(session.getOpenid(), session.getUnionid());
    } catch (WxErrorException e) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "wechat login failed");
    }
  }

  private static WxMaService createService(String appid, String secret) {
    var config = new WxMaDefaultConfigImpl();
    config.setAppid(appid);
    config.setSecret(secret);

    var service = new WxMaServiceImpl();
    service.setWxMaConfig(config);
    return service;
  }
}

