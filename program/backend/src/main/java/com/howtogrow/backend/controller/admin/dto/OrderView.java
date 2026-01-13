package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record OrderView(
    @Schema(description = "订单ID") long orderId,
    @Schema(description = "订单号") String orderNo,
    @Schema(description = "用户ID") long userId,
    @Schema(description = "用户昵称") String userNickname,
    @Schema(description = "用户头像URL") String userAvatarUrl,
    @Schema(description = "套餐ID") long planId,
    @Schema(description = "套餐名称") String planName,
    @Schema(description = "订单金额（分）") int amountCent,
    @Schema(description = "订单状态") String status,
    @Schema(description = "支付平台交易号") String payTradeNo,
    @Schema(description = "微信预支付ID") String prepayId,
    @Schema(description = "创建时间") Instant createdAt,
    @Schema(description = "支付时间") Instant paidAt) {}
