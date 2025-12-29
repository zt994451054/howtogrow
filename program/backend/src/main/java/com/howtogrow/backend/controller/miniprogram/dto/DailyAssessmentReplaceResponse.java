package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "换题响应")
public record DailyAssessmentReplaceResponse(
    @Schema(description = "被替换的题目顺序（1..5）") int displayOrder,
    @Schema(description = "新题目") DailyAssessmentItemView newItem) {}
