package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.MiniprogramMeResponse;
import com.howtogrow.backend.controller.miniprogram.dto.MiniprogramUserView;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class MiniprogramMeService {
  private final BizClock bizClock;
  private final UserAccountRepository userRepo;

  public MiniprogramMeService(BizClock bizClock, UserAccountRepository userRepo) {
    this.bizClock = bizClock;
    this.userRepo = userRepo;
  }

  public MiniprogramMeResponse getMe(long userId) {
    var user = requireActiveUser(userId);
    return new MiniprogramMeResponse(
        new MiniprogramUserView(
            user.id(),
            user.nickname(),
            user.avatarUrl(),
            user.birthDate(),
            user.subscriptionEndAt(),
            user.freeTrialUsed()));
  }

  public void updateProfile(long userId, String nickname, String avatarUrl, LocalDate birthDate) {
    var user = requireActiveUser(userId);
    if (birthDate != null && birthDate.isAfter(bizClock.today())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "出生日期不能是未来时间");
    }
    userRepo.updateProfile(user.id(), nickname, avatarUrl, birthDate);
  }

  private com.howtogrow.backend.infrastructure.user.UserAccount requireActiveUser(long userId) {
    // Token 有效但用户不存在，通常发生在开发环境重置 DB / 清理数据后：此时应当触发小程序静默重新登录，而不是暴露“用户不存在”。
    return userRepo
        .findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "登录已失效，请重新登录"));
  }
}
