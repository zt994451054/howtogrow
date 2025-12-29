package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;

@Schema(description = "开始每日自测（不落库）请求")
public record DailyAssessmentBeginRequest(@Schema(description = "孩子ID") @Positive long childId) {}

