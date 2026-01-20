package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record SubscriptionExtendResponse(
    @Schema(description = "用户ID") long userId,
    @Schema(description = "订阅到期时间（可为空）") Instant subscriptionEndAt) {}

