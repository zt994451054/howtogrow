package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Schema(description = "提交每日自测请求")
public record DailyAssessmentSubmitRequest(
    @Schema(description = "孩子ID") @Positive long childId,
    @Schema(description = "作答列表（必须覆盖 5 题）") @NotEmpty @Valid List<DailyAssessmentAnswerRequest> answers) {}
