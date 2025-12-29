package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "单题作答")
public record DailyAssessmentAnswerRequest(
    @Schema(description = "题目ID") @Positive long questionId,
    @Schema(description = "选中选项ID列表（单选题必须且只能 1 个）") @NotEmpty List<@Positive Long> optionIds) {}
