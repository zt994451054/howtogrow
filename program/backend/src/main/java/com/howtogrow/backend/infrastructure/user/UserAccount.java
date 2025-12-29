package com.howtogrow.backend.infrastructure.user;

import java.time.Instant;

public record UserAccount(
    long id,
    String wechatOpenid,
    String nickname,
    String avatarUrl,
    Instant subscriptionEndAt,
    boolean freeTrialUsed) {}

