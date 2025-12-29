package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "创建订阅订单请求")
public record SubscriptionOrderCreateRequest(@Schema(description = "套餐ID") @Positive long planId) {}
