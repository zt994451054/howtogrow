package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "提交每日自测响应")
public record DailyAssessmentSubmitResponse(
    @Schema(description = "自测记录ID（用于 AI 总结等后续接口）") long assessmentId,
    @Schema(description = "维度得分汇总") List<DimensionScoreView> dimensionScores) {}
