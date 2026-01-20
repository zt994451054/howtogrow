package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "成长报告响应（按 5 个有数据的业务日聚合）")
public record GrowthReportResponse(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "起始日期（YYYY-MM-DD）") LocalDate from,
    @Schema(description = "结束日期（YYYY-MM-DD）") LocalDate to,
    @Schema(description = "得分点（每个点是近 5 个有数据的业务日平均；不足 5 天照算）") List<GrowthDayView> days) {
  @Schema(description = "得分点维度得分")
  public record GrowthDayView(
      @Schema(description = "指标日期（组内最大业务日，YYYY-MM-DD）") LocalDate bizDate,
      @Schema(description = "维度得分列表") List<DimensionScoreView> dimensionScores) {}
}
