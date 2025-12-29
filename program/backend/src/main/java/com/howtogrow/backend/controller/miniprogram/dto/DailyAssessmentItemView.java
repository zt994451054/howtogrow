package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "自测题目（单题）")
public record DailyAssessmentItemView(
    @Schema(description = "题目展示顺序（1..5）") int displayOrder,
    @Schema(description = "题目ID") long questionId,
    @Schema(description = "题目内容") String content,
    @Schema(description = "题型：SINGLE/MULTI") String questionType,
    @Schema(description = "选项列表") List<QuestionOptionView> options) {}
