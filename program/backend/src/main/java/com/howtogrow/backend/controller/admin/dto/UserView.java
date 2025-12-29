package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record UserView(
    @Schema(description = "用户ID") long userId,
    @Schema(description = "微信 openid") String wechatOpenid,
    @Schema(description = "昵称") String nickname,
    @Schema(description = "头像URL") String avatarUrl,
    @Schema(description = "订阅到期时间（可为空）") Instant subscriptionEndAt,
    @Schema(description = "是否已使用免费体验") boolean freeTrialUsed,
    @Schema(description = "创建时间") Instant createdAt) {}
