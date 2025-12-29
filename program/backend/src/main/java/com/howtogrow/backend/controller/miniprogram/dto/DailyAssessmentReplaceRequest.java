package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Schema(description = "换题请求")
public record DailyAssessmentReplaceRequest(
    @Schema(description = "孩子ID") @Positive long childId,
    @Schema(description = "要替换的题目顺序（1..5）") @Min(1) @Max(5) int displayOrder) {}
