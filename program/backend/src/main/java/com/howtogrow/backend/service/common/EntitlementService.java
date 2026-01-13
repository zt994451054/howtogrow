package com.howtogrow.backend.service.common;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class EntitlementService {
  private final UserAccountRepository userRepo;
  private final Clock clock;

  public EntitlementService(UserAccountRepository userRepo, Clock clock) {
    this.userRepo = userRepo;
    this.clock = clock;
  }

  public boolean isSubscribed(long userId) {
    var user =
        userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
    var endAt = user.subscriptionEndAt();
    return endAt != null && endAt.isAfter(Instant.now(clock));
  }

  public void requireCanStartDailyAssessment(long userId) {
    var user =
        userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
    var subscribed = user.subscriptionEndAt() != null && user.subscriptionEndAt().isAfter(Instant.now(clock));
    if (!subscribed && user.freeTrialUsed()) {
      throw new AppException(ErrorCode.FREE_TRIAL_ALREADY_USED, "免费体验已使用，请开通会员");
    }
  }

  public void onDailyAssessmentSubmitted(long userId) {
    var user =
        userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
    var subscribed = user.subscriptionEndAt() != null && user.subscriptionEndAt().isAfter(Instant.now(clock));
    if (!subscribed && !user.freeTrialUsed()) {
      userRepo.markFreeTrialUsed(userId);
    }
  }
}
