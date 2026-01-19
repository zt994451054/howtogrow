package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.auth.Audience;
import com.howtogrow.backend.auth.JwtProperties;
import com.howtogrow.backend.auth.JwtService;
import com.howtogrow.backend.controller.miniprogram.dto.MiniprogramUserView;
import com.howtogrow.backend.controller.miniprogram.dto.WechatLoginResponse;
import com.howtogrow.backend.infrastructure.lock.RedisLockService;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import com.howtogrow.backend.infrastructure.wechat.WechatClient;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

@Service
public class MiniprogramLoginService {
  private static final String DEFAULT_AVATAR_URL =
      "https://howtotalk.oss-cn-beijing.aliyuncs.com/avatar/default_avatar.png";
  private static final Duration CREATE_LOCK_TTL = Duration.ofSeconds(8);
  private static final Duration CREATE_LOCK_WAIT = Duration.ofSeconds(2);
  private static final Duration CREATE_POLL_INTERVAL = Duration.ofMillis(60);

  private final WechatClient wechatClient;
  private final UserAccountRepository userRepo;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final RedisLockService lockService;

  public MiniprogramLoginService(
      WechatClient wechatClient,
      UserAccountRepository userRepo,
      JwtService jwtService,
      JwtProperties jwtProperties,
      RedisLockService lockService) {
    this.wechatClient = wechatClient;
    this.userRepo = userRepo;
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
    this.lockService = lockService;
  }

  public WechatLoginResponse login(String code) {
    var session = wechatClient.exchangeLoginCode(code);
    var user = ensureUserExists(session.openid(), session.unionid());

    var token = jwtService.issue(Audience.MINIPROGRAM, user.id());
    return new WechatLoginResponse(
        token,
        jwtProperties.ttlSeconds(),
        new MiniprogramUserView(
            user.id(),
            user.nickname(),
            user.avatarUrl(),
            user.birthDate(),
            user.subscriptionEndAt(),
            user.freeTrialUsed()));
  }

  private com.howtogrow.backend.infrastructure.user.UserAccount ensureUserExists(
      String openid, String unionid) {
    var existing = userRepo.findByWechatOpenid(openid);
    if (existing.isPresent()) return existing.get();

    String lockKey = "lock:miniprogram:user:create:" + openid;
    var token = lockService.tryLock(lockKey, CREATE_LOCK_TTL);
    if (token.isPresent()) {
      try {
        return userRepo
            .findByWechatOpenid(openid)
            .orElseGet(() -> createUser(openid, unionid));
      } finally {
        lockService.unlock(lockKey, token.get());
      }
    }

    // 另一个并发请求可能正在创建用户：短暂等待并轮询，避免触发唯一键冲突。
    long deadline = System.nanoTime() + CREATE_LOCK_WAIT.toNanos();
    while (System.nanoTime() < deadline) {
      var found = userRepo.findByWechatOpenid(openid);
      if (found.isPresent()) return found.get();
      try {
        Thread.sleep(CREATE_POLL_INTERVAL.toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }

    // 兜底：即使仍未查到，也尝试创建；如遇唯一键冲突则回读。
    return createUserOrReadBack(openid, unionid);
  }

  private com.howtogrow.backend.infrastructure.user.UserAccount createUser(String openid, String unionid) {
    return userRepo.create(
        openid,
        unionid,
        MiniprogramNicknameGenerator.randomNickname(),
        DEFAULT_AVATAR_URL);
  }

  private com.howtogrow.backend.infrastructure.user.UserAccount createUserOrReadBack(
      String openid, String unionid) {
    try {
      return createUser(openid, unionid);
    } catch (DuplicateKeyException e) {
      return userRepo.findByWechatOpenid(openid).orElseThrow(() -> e);
    }
  }
}
