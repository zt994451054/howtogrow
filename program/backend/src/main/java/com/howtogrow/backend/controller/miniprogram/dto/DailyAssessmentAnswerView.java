package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "自测作答（按题汇总）")
public record DailyAssessmentAnswerView(
    @Schema(description = "题目ID") long questionId,
    @Schema(description = "选中选项ID列表") List<Long> optionIds) {}

