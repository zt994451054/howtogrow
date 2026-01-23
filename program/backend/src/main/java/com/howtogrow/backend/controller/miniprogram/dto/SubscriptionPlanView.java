package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "订阅套餐")
public record SubscriptionPlanView(
    @Schema(description = "套餐ID") long planId,
    @Schema(description = "套餐名称") String name,
    @Schema(description = "订阅天数") int days,
    @Schema(description = "原价（分）") int originalPriceCent,
    @Schema(description = "现价（分）") int priceCent) {}
