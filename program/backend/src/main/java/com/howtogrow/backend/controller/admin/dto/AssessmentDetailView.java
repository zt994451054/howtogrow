package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AssessmentDetailView(
    @Schema(description = "自测ID") long assessmentId,
    @Schema(description = "用户ID") long userId,
    @Schema(description = "用户昵称") String userNickname,
    @Schema(description = "用户头像URL") String userAvatarUrl,
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "孩子昵称") String childNickname,
    @Schema(description = "提交日期（yyyy-MM-dd，北京时间口径）") LocalDate bizDate,
    @Schema(description = "提交时间") Instant submittedAt,
    @Schema(description = "AI 总结（可为空）") String aiSummary,
    @Schema(description = "维度得分（固定 5 维度）") List<AssessmentDimensionScoreView> dimensionScores,
    @Schema(description = "题目与作答明细（按展示顺序）") List<ItemView> items) {
  public record ItemView(
      @Schema(description = "题目展示顺序（1..10）") int displayOrder,
      @Schema(description = "问题ID") long questionId,
      @Schema(description = "题型：SINGLE/MULTI") String questionType,
      @Schema(description = "题目内容") String questionContent,
      @Schema(description = "选项列表（按 sortNo 升序）") List<OptionView> options,
      @Schema(description = "已选中的选项ID列表") List<Long> selectedOptionIds) {}

  public record OptionView(
      @Schema(description = "选项ID") long optionId,
      @Schema(description = "选项内容") String content,
      @Schema(description = "是否建议：0否 1是") int suggestFlag,
      @Schema(description = "改进建议（可为空）") String improvementTip,
      @Schema(description = "排序号（越小越靠前）") int sortNo) {}
}

