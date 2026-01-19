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
      throw new AppException(ErrorCode.INVALID_REQUEST, "订单号不能为空");
    }
    if (amountCent <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "金额必须为正数");
    }
    if (payerOpenid == null || payerOpenid.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "用户 openid 不能为空");
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

      // WxJava 的 createOrderV3 是泛型方法，JSAPI 场景会直接返回 JsapiResult（而非 WxPayUnifiedOrderV3Result），
      // 之前强行接成 WxPayUnifiedOrderV3Result 会触发运行时 ClassCastException。
      WxPayUnifiedOrderV3Result.JsapiResult payInfo = service.createOrderV3(TradeTypeEnum.JSAPI, req);
      if (payInfo == null || payInfo.getPaySign() == null || payInfo.getPaySign().isBlank()) {
        throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付下单失败");
      }

      return new PayParams(
          payInfo.getTimeStamp(),
          payInfo.getNonceStr(),
          payInfo.getPackageValue(),
          payInfo.getSignType(),
          payInfo.getPaySign());
    } catch (WxPayException e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付下单失败");
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
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：mchId");
    }
    if (payProps.mchSerialNo() == null || payProps.mchSerialNo().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：mchSerialNo");
    }
    if (payProps.apiV3Key() == null || payProps.apiV3Key().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：apiV3Key");
    }
    if (payProps.privateKeyPath() == null || payProps.privateKeyPath().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：privateKeyPath");
    }
    if (payProps.notifyUrl() == null || payProps.notifyUrl().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：notifyUrl");
    }
    if (wechatProps.appid() == null || wechatProps.appid().isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：appId");
    }

    var config = new WxPayConfig();
    config.setAppId(wechatProps.appid());
    config.setMchId(payProps.mchId());
    config.setCertSerialNo(payProps.mchSerialNo());
    config.setApiV3Key(payProps.apiV3Key());
    config.setPrivateKeyPath(payProps.privateKeyPath());
    config.setNotifyUrl(payProps.notifyUrl());

    var verifyMode =
        payProps.verifyMode() == null ? WechatPayVerifyMode.PLATFORM_PUBLIC_KEY : payProps.verifyMode();
    if (verifyMode == WechatPayVerifyMode.PLATFORM_PUBLIC_KEY) {
      if (payProps.publicKeyId() == null || payProps.publicKeyId().isBlank()) {
        throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：publicKeyId");
      }
      if (payProps.publicKeyPath() == null || payProps.publicKeyPath().isBlank()) {
        throw new AppException(ErrorCode.INTERNAL_ERROR, "微信支付配置缺失：publicKeyPath");
      }
      config.setFullPublicKeyModel(true);
      config.setPublicKeyId(payProps.publicKeyId());
      config.setPublicKeyPath(payProps.publicKeyPath());
    }

    var service = new WxPayServiceImpl();
    service.setConfig(config);
    return service;
  }
}
