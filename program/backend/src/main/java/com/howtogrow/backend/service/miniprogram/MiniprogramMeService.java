package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.MiniprogramMeResponse;
import com.howtogrow.backend.controller.miniprogram.dto.MiniprogramUserView;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class MiniprogramMeService {
  private final UserAccountRepository userRepo;

  public MiniprogramMeService(UserAccountRepository userRepo) {
    this.userRepo = userRepo;
  }

  public MiniprogramMeResponse getMe(long userId) {
    var user = userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
    return new MiniprogramMeResponse(
        new MiniprogramUserView(
            user.id(),
            user.nickname(),
            user.avatarUrl(),
            user.subscriptionEndAt(),
            user.freeTrialUsed()));
  }

  public void updateProfile(long userId, String nickname, String avatarUrl) {
    var user = userRepo.findById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "用户不存在"));
    userRepo.updateProfile(user.id(), nickname, avatarUrl);
  }
}
