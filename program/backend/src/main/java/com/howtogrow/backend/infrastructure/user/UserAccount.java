package com.howtogrow.backend.infrastructure.user;

import java.time.Instant;
import java.time.LocalDate;

public record UserAccount(
    long id,
    String wechatOpenid,
    String nickname,
    String avatarUrl,
    LocalDate birthDate,
    Instant subscriptionEndAt,
    boolean freeTrialUsed) {}
