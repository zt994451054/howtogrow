package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "小程序端用户视图")
public record MiniprogramUserView(
    @Schema(description = "用户ID") long id,
    @Schema(description = "昵称") String nickname,
    @Schema(description = "头像URL") String avatarUrl,
    @Schema(description = "订阅到期时间（为空表示未订阅）") Instant subscriptionEndAt,
    @Schema(description = "是否已使用免费体验") boolean freeTrialUsed) {}
