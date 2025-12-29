package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.auth.Audience;
import com.howtogrow.backend.auth.JwtProperties;
import com.howtogrow.backend.auth.JwtService;
import com.howtogrow.backend.controller.miniprogram.dto.MiniprogramUserView;
import com.howtogrow.backend.controller.miniprogram.dto.WechatLoginResponse;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import com.howtogrow.backend.infrastructure.wechat.WechatClientProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MiniprogramLoginService {
  private final WechatClientProvider wechatClientProvider;
  private final UserAccountRepository userRepo;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;

  public MiniprogramLoginService(
      WechatClientProvider wechatClientProvider,
      UserAccountRepository userRepo,
      JwtService jwtService,
      JwtProperties jwtProperties) {
    this.wechatClientProvider = wechatClientProvider;
    this.userRepo = userRepo;
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
  }

  @Transactional
  public WechatLoginResponse login(String code) {
    var session = wechatClientProvider.get().exchangeLoginCode(code);
    var user =
        userRepo
            .findByWechatOpenid(session.openid())
            .orElseGet(() -> userRepo.create(session.openid(), session.unionid()));

    var token = jwtService.issue(Audience.MINIPROGRAM, user.id());
    return new WechatLoginResponse(
        token,
        jwtProperties.ttlSeconds(),
        new MiniprogramUserView(
            user.id(),
            user.nickname(),
            user.avatarUrl(),
            user.subscriptionEndAt(),
            user.freeTrialUsed()));
  }
}

