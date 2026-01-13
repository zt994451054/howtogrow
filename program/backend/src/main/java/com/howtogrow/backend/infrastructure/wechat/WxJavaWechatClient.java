package com.howtogrow.backend.infrastructure.wechat;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.api.exception.AppException;
import me.chanjar.weixin.common.error.WxErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WxJavaWechatClient implements WechatClient {
  private static final Logger log = LoggerFactory.getLogger(WxJavaWechatClient.class);
  private final WechatProperties props;

  public WxJavaWechatClient(WechatProperties props) {
    this.props = props;
  }

  @Override
  public WechatSession exchangeLoginCode(String code) {
    if (code == null || code.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "code 不能为空");
    }
    if (props.appid() == null
        || props.appid().isBlank()
        || props.secret() == null
        || props.secret().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信配置缺失");
    }

    try {
      var service = createService(props.appid(), props.secret());
      var session = service.getUserService().getSessionInfo(code);
      if (session == null || session.getOpenid() == null || session.getOpenid().isBlank()) {
        throw new AppException(ErrorCode.UNAUTHORIZED, "微信登录失败");
      }
      return new WechatSession(session.getOpenid(), session.getUnionid());
    } catch (WxErrorException e) {
      var wxErr = e.getError();
      if (wxErr != null) {
        log.warn(
            "Wechat login failed: traceId={} errCode={} errMsg={}",
            TraceId.current(),
            wxErr.getErrorCode(),
            wxErr.getErrorMsg());
      } else {
        log.warn("Wechat login failed: traceId={} (no wx error payload)", TraceId.current());
      }
      throw new AppException(ErrorCode.UNAUTHORIZED, "微信登录失败");
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
