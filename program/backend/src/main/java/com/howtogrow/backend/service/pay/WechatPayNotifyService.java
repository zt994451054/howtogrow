package com.howtogrow.backend.service.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.pay.WxJavaWechatPayClient;
import com.howtogrow.backend.infrastructure.pay.WechatPayTransactionRepository;
import com.howtogrow.backend.infrastructure.subscription.PurchaseOrderRepository;
import com.howtogrow.backend.infrastructure.subscription.SubscriptionGrantRepository;
import com.howtogrow.backend.infrastructure.subscription.SubscriptionPlanRepository;
import com.howtogrow.backend.infrastructure.subscription.UserSubscriptionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WechatPayNotifyService {
  private final PurchaseOrderRepository orderRepo;
  private final SubscriptionPlanRepository planRepo;
  private final UserSubscriptionRepository userSubscriptionRepo;
  private final SubscriptionGrantRepository grantRepo;
  private final WechatPayTransactionRepository txnRepo;
  private final WxJavaWechatPayClient wxJavaWechatPayClient;
  private final Clock clock;

  public WechatPayNotifyService(
      PurchaseOrderRepository orderRepo,
      SubscriptionPlanRepository planRepo,
      UserSubscriptionRepository userSubscriptionRepo,
      SubscriptionGrantRepository grantRepo,
      WechatPayTransactionRepository txnRepo,
      WxJavaWechatPayClient wxJavaWechatPayClient,
      Clock clock) {
    this.orderRepo = orderRepo;
    this.planRepo = planRepo;
    this.userSubscriptionRepo = userSubscriptionRepo;
    this.grantRepo = grantRepo;
    this.txnRepo = txnRepo;
    this.wxJavaWechatPayClient = wxJavaWechatPayClient;
    this.clock = clock;
  }

  /**
   * 使用 WxJava 验签并解密回调 resource（WeChat Pay v3）。
   */
  @Transactional
  public void process(String eventId, String rawBody, JsonNode rawNode, SignatureHeader signatureHeader) {
    if (signatureHeader == null
        || signatureHeader.getTimeStamp() == null
        || signatureHeader.getTimeStamp().isBlank()
        || signatureHeader.getNonce() == null
        || signatureHeader.getNonce().isBlank()
        || signatureHeader.getSignature() == null
        || signatureHeader.getSignature().isBlank()
        || signatureHeader.getSerial() == null
        || signatureHeader.getSerial().isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "缺少微信支付验签请求头");
    }
    if (rawBody == null || rawBody.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "请求体为空");
    }

    try {
      var wxPayService = wxJavaWechatPayClient.requireWxPayService();
      var parsed = wxPayService.parseOrderNotifyV3Result(rawBody, signatureHeader);
      var r = parsed == null ? null : parsed.getResult();
      if (r == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "微信支付回调数据不合法");
      }

      var outTradeNo = requiredText(r.getOutTradeNo(), "out_trade_no");
      var transactionId = requiredText(r.getTransactionId(), "transaction_id");
      var tradeState = requiredText(r.getTradeState(), "trade_state");
      if (!"SUCCESS".equalsIgnoreCase(tradeState)) {
        return;
      }

      var totalCent = requiredInt(r.getAmount() == null ? null : r.getAmount().getTotal(), "amount.total");
      var currency = r.getAmount() == null ? "CNY" : textOrDefault(r.getAmount().getCurrency(), "CNY");

      var payerOpenid = "";
      if (r.getPayer() != null && r.getPayer().getOpenid() != null) {
        payerOpenid = r.getPayer().getOpenid();
      }

      var successTime = parseSuccessTime(r.getSuccessTime());
      if (successTime == null) {
        successTime = Instant.now(clock);
      }

      processPaidTransaction(
          eventId,
          outTradeNo,
          transactionId,
          tradeState,
          textOrEmpty(r.getMchid()),
          textOrEmpty(r.getAppid()),
          textOrEmpty(r.getTradeType()),
          textOrEmpty(r.getTradeStateDesc()),
          textOrEmpty(r.getBankType()),
          payerOpenid,
          totalCent,
          currency,
          successTime);
    } catch (WxPayException e) {
      throw new AppException(ErrorCode.WECHAT_PAY_VERIFY_FAILED, "微信支付验签失败");
    }
  }

  private void processPaidTransaction(
      String eventId,
      String outTradeNo,
      String transactionId,
      String tradeState,
      String mchId,
      String appId,
      String tradeType,
      String tradeStateDesc,
      String bankType,
      String payerOpenid,
      int totalCent,
      String currency,
      Instant successTime) {
    var order =
        orderRepo
            .findByOrderNo(outTradeNo)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "订单不存在"));
    if (totalCent != order.amountCent()) {
      throw new AppException(ErrorCode.WECHAT_PAY_AMOUNT_MISMATCH, "支付金额不匹配");
    }

    var plan =
        planRepo
            .findById(order.planId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "套餐不存在"));

    txnRepo.upsertTransaction(
        order.id(),
        outTradeNo,
        mchId,
        appId,
        transactionId,
        tradeType,
        tradeState,
        tradeStateDesc,
        bankType,
        payerOpenid,
        totalCent,
        currency,
        successTime,
        eventId);

    orderRepo.markPaid(order.id(), transactionId, successTime);

    var currentEndAt = userSubscriptionRepo.findSubscriptionEndAt(order.userId()).orElse(null);
    var grantedFrom =
        currentEndAt == null || currentEndAt.isBefore(successTime) ? successTime : currentEndAt;
    var grantedTo = grantedFrom.plus(Duration.ofDays(plan.days()));

    var inserted =
        grantRepo.insertIfAbsent(
            order.userId(), order.id(), order.planId(), plan.days(), grantedFrom, grantedTo);
    if (inserted) {
      userSubscriptionRepo.updateSubscriptionEndAt(order.userId(), grantedTo);
    }
  }

  private static String requiredText(JsonNode node, String field) {
    var text = textOrEmpty(node, field);
    if (text.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "缺少字段：" + field);
    }
    return text;
  }

  private static int requiredInt(JsonNode node, String field) {
    if (node == null || node.isNull()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "缺少字段：" + field);
    }
    var value = node.get(field);
    if (value == null || !value.isInt()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "字段必须为整数：" + field);
    }
    return value.asInt();
  }

  private static String textOrEmpty(JsonNode node, String field) {
    if (node == null) {
      return "";
    }
    var value = node.get(field);
    return value == null || value.isNull() ? "" : Objects.toString(value.asText(), "");
  }

  private static String textOrDefault(JsonNode node, String field, String fallback) {
    var text = textOrEmpty(node, field);
    return text.isBlank() ? fallback : text;
  }

  private static Instant parseSuccessTime(JsonNode node) {
    if (node == null || node.isNull()) {
      return null;
    }
    var text = node.asText();
    if (text == null || text.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(text).toInstant();
    } catch (Exception ignored) {
      return null;
    }
  }

  private static String requiredText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " is required");
    }
    return value;
  }

  private static int requiredInt(Integer value, String field) {
    if (value == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " is required");
    }
    if (value <= 0) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " must be positive");
    }
    return value;
  }

  private static String textOrEmpty(String value) {
    return value == null ? "" : value.trim();
  }

  private static String textOrDefault(String value, String fallback) {
    var t = textOrEmpty(value);
    return t.isBlank() ? fallback : t;
  }

  private static Instant parseSuccessTime(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (Exception ignored) {
      return null;
    }
  }
}
