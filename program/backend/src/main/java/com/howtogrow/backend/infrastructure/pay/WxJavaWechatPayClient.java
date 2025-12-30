package com.howtogrow.backend.infrastructure.pay;

import com.github.binarywang.wxpay.bean.request.WxPayUnifiedOrderV3Request;
import com.github.binarywang.wxpay.bean.result.WxPayUnifiedOrderV3Result;
import com.github.binarywang.wxpay.bean.result.enums.TradeTypeEnum;
import com.github.binarywang.wxpay.config.WxPayConfig;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import com.github.binarywang.wxpay.service.impl.WxPayServiceImpl;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.wechat.WechatProperties;
import org.springframework.stereotype.Component;

@Component
public class WxJavaWechatPayClient implements WechatPayClient {
  private final WechatPayProperties payProps;
  private final WechatProperties wechatProps;

  private volatile WxPayService wxPayService;

  public WxJavaWechatPayClient(WechatPayProperties payProps, WechatProperties wechatProps) {
    this.payProps = payProps;
    this.wechatProps = wechatProps;
  }

  @Override
  public PayParams createJsapiPayParams(String orderNo, int amountCent, String payerOpenid) {
    if (orderNo == null || orderNo.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "orderNo is required");
    }
    if (amountCent <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "amountCent must be positive");
    }
    if (payerOpenid == null || payerOpenid.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "payerOpenid is required");
    }

    var service = requireWxPayService();
    var cfg = (WxPayServiceImpl) service;
    var config = cfg.getConfig();

    try {
      var req =
          new WxPayUnifiedOrderV3Request()
              .setAppid(config.getAppId())
              .setMchid(config.getMchId())
              .setDescription("Subscription")
              .setOutTradeNo(orderNo)
              .setNotifyUrl(config.getNotifyUrl())
              .setAmount(new WxPayUnifiedOrderV3Request.Amount().setTotal(amountCent).setCurrency("CNY"))
              .setPayer(new WxPayUnifiedOrderV3Request.Payer().setOpenid(payerOpenid));

      WxPayUnifiedOrderV3Result res = service.createOrderV3(TradeTypeEnum.JSAPI, req);
      if (res == null || res.getPrepayId() == null || res.getPrepayId().isBlank()) {
        throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay create order failed");
      }

      var payInfo =
          (WxPayUnifiedOrderV3Result.JsapiResult)
              res.getPayInfo(TradeTypeEnum.JSAPI, config.getAppId(), null, config.getPrivateKey());
      return new PayParams(
          payInfo.getTimeStamp(),
          payInfo.getNonceStr(),
          payInfo.getPackageValue(),
          payInfo.getSignType(),
          payInfo.getPaySign());
    } catch (WxPayException e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay create order failed");
    }
  }

  public WxPayService requireWxPayService() {
    var existing = wxPayService;
    if (existing != null) {
      return existing;
    }
    synchronized (this) {
      if (wxPayService != null) {
        return wxPayService;
      }
      wxPayService = createWxPayService();
      return wxPayService;
    }
  }

  private WxPayService createWxPayService() {
    if (payProps.mchId() == null || payProps.mchId().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay mchId missing");
    }
    if (payProps.mchSerialNo() == null || payProps.mchSerialNo().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay mchSerialNo missing");
    }
    if (payProps.apiV3Key() == null || payProps.apiV3Key().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay apiV3Key missing");
    }
    if (payProps.privateKeyPath() == null || payProps.privateKeyPath().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay privateKeyPath missing");
    }
    if (payProps.notifyUrl() == null || payProps.notifyUrl().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay notifyUrl missing");
    }
    if (wechatProps.appid() == null || wechatProps.appid().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat appid missing");
    }

    var config = new WxPayConfig();
    config.setAppId(wechatProps.appid());
    config.setMchId(payProps.mchId());
    config.setCertSerialNo(payProps.mchSerialNo());
    config.setApiV3Key(payProps.apiV3Key());
    config.setPrivateKeyPath(payProps.privateKeyPath());
    config.setNotifyUrl(payProps.notifyUrl());

    var service = new WxPayServiceImpl();
    service.setConfig(config);
    return service;
  }
}
