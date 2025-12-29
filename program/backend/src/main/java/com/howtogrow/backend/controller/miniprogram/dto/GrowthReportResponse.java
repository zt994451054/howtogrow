package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "成长报告响应（按天聚合）")
public record GrowthReportResponse(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "起始日期（YYYY-MM-DD）") LocalDate from,
    @Schema(description = "结束日期（YYYY-MM-DD）") LocalDate to,
    @Schema(description = "每日得分") List<GrowthDayView> days) {
  @Schema(description = "单日维度得分")
  public record GrowthDayView(
      @Schema(description = "业务日期（YYYY-MM-DD）") LocalDate bizDate,
      @Schema(description = "维度得分列表") List<DimensionScoreView> dimensionScores) {}
}
