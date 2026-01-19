package com.howtogrow.backend.service.miniprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.howtogrow.backend.auth.JwtProperties;
import com.howtogrow.backend.auth.JwtService;
import com.howtogrow.backend.controller.miniprogram.dto.WechatLoginResponse;
import com.howtogrow.backend.infrastructure.lock.RedisLockService;
import com.howtogrow.backend.infrastructure.user.UserAccount;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import com.howtogrow.backend.infrastructure.wechat.WechatClient;
import com.howtogrow.backend.infrastructure.wechat.WechatSession;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MiniprogramLoginServiceConcurrencyTest {
  @Test
  void login_concurrentCalls_createOnlyOnce() throws Exception {
    var wechatClient = mock(WechatClient.class);
    when(wechatClient.exchangeLoginCode("c1")).thenReturn(new WechatSession("openid-1", null));
    when(wechatClient.exchangeLoginCode("c2")).thenReturn(new WechatSession("openid-1", null));

    var userRepo = mock(UserAccountRepository.class);
    var created = new AtomicInteger(0);
    var createdUser =
        new UserAccount(
            1L, "openid-1", "用户A", "a", null, null, false);

    // First read: empty; after create: present.
    when(userRepo.findByWechatOpenid("openid-1"))
        .thenAnswer(
            inv -> created.get() > 0 ? Optional.of(createdUser) : Optional.empty());
    when(userRepo.create(
            org.mockito.ArgumentMatchers.eq("openid-1"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString()))
        .thenAnswer(
            inv -> {
              created.incrementAndGet();
              return createdUser;
            });

    var lockService = mock(RedisLockService.class);
    // Only first call gets the lock; second will wait and then read back user.
    var lockUsed = new AtomicBoolean(false);
    when(lockService.tryLock(
            org.mockito.ArgumentMatchers.eq("lock:miniprogram:user:create:openid-1"),
            org.mockito.ArgumentMatchers.any(Duration.class)))
        .thenAnswer(
            inv -> lockUsed.compareAndSet(false, true) ? Optional.of("t1") : Optional.empty());

    var clock = Clock.fixed(Instant.parse("2026-01-18T00:00:00Z"), ZoneOffset.UTC);
    var jwtProps = new JwtProperties("minisecret".repeat(8), "adminsecret".repeat(8), 3600);
    var jwtService = new JwtService(jwtProps, clock);
    var service = new MiniprogramLoginService(wechatClient, userRepo, jwtService, jwtProps, lockService);

    var pool = Executors.newFixedThreadPool(2);
    try {
      CountDownLatch start = new CountDownLatch(1);
      var f1 =
          pool.submit(
              () -> {
                start.await();
                return service.login("c1");
              });
      var f2 =
          pool.submit(
              () -> {
                start.await();
                return service.login("c2");
              });
      start.countDown();

      WechatLoginResponse r1 = f1.get(3, TimeUnit.SECONDS);
      WechatLoginResponse r2 = f2.get(3, TimeUnit.SECONDS);
      assertEquals(r1.user().id(), r2.user().id());
    } finally {
      pool.shutdownNow();
    }

    verify(userRepo, times(1))
        .create(
            org.mockito.ArgumentMatchers.eq("openid-1"),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString());
  }
}
