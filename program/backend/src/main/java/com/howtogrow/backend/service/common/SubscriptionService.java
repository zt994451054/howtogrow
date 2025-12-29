package com.howtogrow.backend.service.common;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {
  private final UserAccountRepository userRepo;
  private final Clock clock;

  public SubscriptionService(UserAccountRepository userRepo, Clock clock) {
    this.userRepo = userRepo;
    this.clock = clock;
  }

  public void requireSubscribed(long userId) {
    var user =
        userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "user not found"));
    var endAt = user.subscriptionEndAt();
    if (endAt == null || endAt.isBefore(Instant.now(clock))) {
      throw new AppException(ErrorCode.SUBSCRIPTION_REQUIRED, "subscription required");
    }
  }
}
