package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record PlanView(
    @Schema(description = "套餐ID") long planId,
    @Schema(description = "套餐名称") String name,
    @Schema(description = "套餐天数") int days,
    @Schema(description = "价格（分）") int priceCent,
    @Schema(description = "状态：0禁用 1启用") int status) {}
