package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "开始每日自测（不落库）响应")
public record DailyAssessmentBeginResponse(
    @Schema(description = "自测会话ID（后续换题/提交使用）") String sessionId,
    @Schema(description = "题目列表（5-10题）") List<DailyAssessmentItemView> items) {}
