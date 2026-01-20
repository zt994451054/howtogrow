package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.UserView;
import com.howtogrow.backend.infrastructure.admin.UserQueryRepository;
import com.howtogrow.backend.infrastructure.subscription.UserSubscriptionRepository;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {
  private final UserQueryRepository queryRepo;
  private final UserAccountRepository userRepo;
  private final UserSubscriptionRepository subscriptionRepo;
  private final Clock clock;

  public AdminUserService(
      UserQueryRepository queryRepo,
      UserAccountRepository userRepo,
      UserSubscriptionRepository subscriptionRepo,
      Clock clock) {
    this.queryRepo = queryRepo;
    this.userRepo = userRepo;
    this.subscriptionRepo = subscriptionRepo;
    this.clock = clock;
  }

  public PageResponse<UserView> list(
      int page,
      int pageSize,
      Long userId,
      String keyword,
      Boolean freeTrialUsed,
      String subscriptionStatus) {
    int offset = (page - 1) * pageSize;
    long total = queryRepo.countUsers(userId, keyword, freeTrialUsed, subscriptionStatus);
    var items =
        queryRepo.listUsers(offset, pageSize, userId, keyword, freeTrialUsed, subscriptionStatus).stream()
            .map(
                u ->
                    new UserView(
                        u.id(),
                        u.wechatOpenid(),
                        u.nickname(),
                        u.avatarUrl(),
                        u.subscriptionEndAt(),
                        u.freeTrialUsed(),
                        u.createdAt()))
            .toList();
    return new PageResponse<>(page, pageSize, total, items);
  }

  public Instant extendSubscription(long userId, int days) {
    var user =
        userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));

    var now = Instant.now(clock);
    var currentEndAt = user.subscriptionEndAt();
    var grantedFrom = currentEndAt == null || currentEndAt.isBefore(now) ? now : currentEndAt;
    var grantedTo = grantedFrom.plus(Duration.ofDays(days));

    subscriptionRepo.updateSubscriptionEndAt(userId, grantedTo);
    return grantedTo;
  }
}
