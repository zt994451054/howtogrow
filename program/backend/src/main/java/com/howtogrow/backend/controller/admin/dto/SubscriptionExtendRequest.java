package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SubscriptionExtendRequest(
    @Schema(description = "延长天数（在当前订阅基础上增加）") @Min(1) @Max(3650) int days) {}

