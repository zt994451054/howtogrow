package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "每日觉察-月度概览")
public record MonthlyAwarenessResponse(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "月份（YYYY-MM）") String month,
    @Schema(description = "该月每日数据（按日期升序）") List<MonthlyAwarenessDayView> days) {

  @Schema(description = "每日觉察-单日概览")
  public record MonthlyAwarenessDayView(
      @Schema(description = "记录日期（YYYY-MM-DD）") LocalDate recordDate,
      @Schema(description = "育儿状态（文本；为空表示未记录）") String parentingStatusCode,
      @Schema(description = "育儿状态表情ID（用于客户端渲染；为空表示未记录）") String parentingStatusMoodId,
      @Schema(description = "烦恼场景（为空表示未记录）") List<MonthlyAwarenessTroubleSceneView> troubleScenes,
      @Schema(description = "自测结果（为空表示未完成）") MonthlyAwarenessAssessmentView assessment,
      @Schema(description = "育儿日记（为空表示未记录）") MonthlyAwarenessDiaryView diary) {}

  @Schema(description = "烦恼场景（用于每日觉察月视图）")
  public record MonthlyAwarenessTroubleSceneView(
      @Schema(description = "场景ID") long id,
      @Schema(description = "场景名称") String name,
      @Schema(description = "Logo URL") String logoUrl) {}

  @Schema(description = "每日自测结果（用于每日觉察月视图）")
  public record MonthlyAwarenessAssessmentView(
      @Schema(description = "自测记录ID") long assessmentId,
      @Schema(description = "提交时间（ISO-8601，+08:00）") String submittedAt,
      @Schema(description = "AI 总结（可选）") String aiSummary) {}

  @Schema(description = "育儿日记（用于每日觉察月视图）")
  public record MonthlyAwarenessDiaryView(
      @Schema(description = "日记内容") String content,
      @Schema(description = "配图 URL（可选）") String imageUrl) {}
}
