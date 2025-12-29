package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PlanUpdateRequest(
    @Schema(description = "套餐名称") @NotBlank String name,
    @Schema(description = "套餐天数") @NotNull @Min(1) Integer days,
    @Schema(description = "价格（分）") @NotNull @Min(0) Integer priceCent,
    @Schema(description = "状态：0禁用 1启用") @NotNull @Min(0) @Max(1) Integer status) {}
