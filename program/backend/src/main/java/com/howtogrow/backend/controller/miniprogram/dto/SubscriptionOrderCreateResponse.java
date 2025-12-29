package com.howtogrow.backend.controller.miniprogram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建订阅订单响应")
public record SubscriptionOrderCreateResponse(
    @Schema(description = "业务订单号") String orderNo,
    @Schema(description = "支付参数（wx.requestPayment）") PayParams payParams) {
  @Schema(description = "wx.requestPayment 参数")
  public record PayParams(
      @Schema(description = "时间戳（秒）") String timeStamp,
      @Schema(description = "随机串") String nonceStr,
      @Schema(description = "package 字段（通常为 prepay_id=...）") @JsonProperty("package") String packageValue,
      @Schema(description = "签名类型（通常 RSA）") String signType,
      @Schema(description = "签名") String paySign) {}
}
