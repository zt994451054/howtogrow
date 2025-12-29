package com.howtogrow.backend.service.pay;

import com.fasterxml.jackson.databind.JsonNode;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.pay.WechatPayProperties;
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
  private final WechatPayProperties props;
  private final PurchaseOrderRepository orderRepo;
  private final SubscriptionPlanRepository planRepo;
  private final UserSubscriptionRepository userSubscriptionRepo;
  private final SubscriptionGrantRepository grantRepo;
  private final WechatPayTransactionRepository txnRepo;
  private final Clock clock;

  public WechatPayNotifyService(
      WechatPayProperties props,
      PurchaseOrderRepository orderRepo,
      SubscriptionPlanRepository planRepo,
      UserSubscriptionRepository userSubscriptionRepo,
      SubscriptionGrantRepository grantRepo,
      WechatPayTransactionRepository txnRepo,
      Clock clock) {
    this.props = props;
    this.orderRepo = orderRepo;
    this.planRepo = planRepo;
    this.userSubscriptionRepo = userSubscriptionRepo;
    this.grantRepo = grantRepo;
    this.txnRepo = txnRepo;
    this.clock = clock;
  }

  /**
   * 当前实现支持 mock 回调：在 `app.wechat-pay.mock-enabled=true` 时，body 可直接带交易字段。
   * 生产环境需要补齐：验签、解密回调 resource（WeChat Pay v3）。
   */
  @Transactional
  public void process(String eventId, JsonNode rawNode) {
    if (!props.mockEnabled()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "wechat pay verify/decrypt not implemented");
    }

    var outTradeNo = requiredText(rawNode, "out_trade_no");
    var transactionId = requiredText(rawNode, "transaction_id");
    var tradeState = requiredText(rawNode, "trade_state");
    if (!"SUCCESS".equalsIgnoreCase(tradeState)) {
      return;
    }

    var order =
        orderRepo
            .findByOrderNo(outTradeNo)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "order not found"));

    var amountNode = rawNode.get("amount");
    var totalCent = requiredInt(amountNode, "total");
    if (totalCent != order.amountCent()) {
      throw new AppException(ErrorCode.WECHAT_PAY_AMOUNT_MISMATCH, "amount mismatch");
    }

    var plan =
        planRepo
            .findById(order.planId())
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "plan not found"));

    var successTime = parseSuccessTime(rawNode.get("success_time"));
    if (successTime == null) {
      successTime = Instant.now(clock);
    }

    txnRepo.upsertTransaction(
        order.id(),
        outTradeNo,
        textOrEmpty(rawNode, "mchid"),
        textOrEmpty(rawNode, "appid"),
        transactionId,
        textOrEmpty(rawNode, "trade_type"),
        tradeState,
        textOrEmpty(rawNode, "trade_state_desc"),
        textOrEmpty(rawNode, "bank_type"),
        textOrEmpty(rawNode.path("payer"), "openid"),
        totalCent,
        textOrDefault(amountNode, "currency", "CNY"),
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
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " is required");
    }
    return text;
  }

  private static int requiredInt(JsonNode node, String field) {
    if (node == null || node.isNull()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " is required");
    }
    var value = node.get(field);
    if (value == null || !value.isInt()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " is required");
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
}
