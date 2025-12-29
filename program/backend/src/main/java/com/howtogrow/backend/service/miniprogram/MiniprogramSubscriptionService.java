package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.SubscriptionOrderCreateResponse;
import com.howtogrow.backend.controller.miniprogram.dto.SubscriptionPlanView;
import com.howtogrow.backend.infrastructure.pay.WechatPayClientProvider;
import com.howtogrow.backend.infrastructure.subscription.PurchaseOrderRepository;
import com.howtogrow.backend.infrastructure.subscription.SubscriptionPlanRepository;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiniprogramSubscriptionService {
  private static final DateTimeFormatter ORDER_DATE_FMT =
      DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.of("Asia/Shanghai"));

  private final SubscriptionPlanRepository planRepo;
  private final PurchaseOrderRepository orderRepo;
  private final UserAccountRepository userRepo;
  private final WechatPayClientProvider wechatPayClientProvider;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public MiniprogramSubscriptionService(
      SubscriptionPlanRepository planRepo,
      PurchaseOrderRepository orderRepo,
      UserAccountRepository userRepo,
      WechatPayClientProvider wechatPayClientProvider,
      Clock clock) {
    this.planRepo = planRepo;
    this.orderRepo = orderRepo;
    this.userRepo = userRepo;
    this.wechatPayClientProvider = wechatPayClientProvider;
    this.clock = clock;
  }

  public List<SubscriptionPlanView> listPlans() {
    return planRepo.listActivePlans().stream()
        .map(p -> new SubscriptionPlanView(p.planId(), p.name(), p.days(), p.priceCent()))
        .toList();
  }

  @Transactional
  public SubscriptionOrderCreateResponse createOrder(long userId, long planId) {
    var user =
        userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "user not found"));
    var plan = planRepo.findActiveById(planId).orElse(null);
    if (plan == null || plan.days() <= 0 || plan.priceCent() < 0) {
      throw new AppException(ErrorCode.NOT_FOUND, "plan not found");
    }

    var orderNo = generateOrderNo(userId);
    var payParams =
        wechatPayClientProvider
            .get()
            .createJsapiPayParams(orderNo, plan.priceCent(), user.wechatOpenid());

    var prepayId = extractPrepayId(payParams.packageValue());
    orderRepo.createOrder(orderNo, userId, planId, plan.priceCent(), prepayId);

    return new SubscriptionOrderCreateResponse(
        orderNo,
        new SubscriptionOrderCreateResponse.PayParams(
            payParams.timeStamp(),
            payParams.nonceStr(),
            payParams.packageValue(),
            payParams.signType(),
            payParams.paySign()));
  }

  private String generateOrderNo(long userId) {
    var datePart = ORDER_DATE_FMT.format(Instant.now(clock));
    for (int i = 0; i < 5; i++) {
      var suffix = HexFormat.of().formatHex(randomBytes(6));
      var orderNo = "O" + datePart + "U" + userId + suffix;
      if (!orderRepo.existsOrderNo(orderNo)) {
        return orderNo;
      }
    }
    throw new AppException(ErrorCode.INTERNAL_ERROR, "orderNo generation failed");
  }

  private byte[] randomBytes(int size) {
    var bytes = new byte[size];
    random.nextBytes(bytes);
    return bytes;
  }

  private static String extractPrepayId(String packageValue) {
    if (packageValue == null) {
      return null;
    }
    var prefix = "prepay_id=";
    var idx = packageValue.indexOf(prefix);
    if (idx < 0) {
      return packageValue;
    }
    return packageValue.substring(idx + prefix.length());
  }
}
