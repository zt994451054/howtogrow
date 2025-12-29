package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record QuestionDetailView(
    @Schema(description = "题目ID") long questionId,
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") int minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") int maxAge,
    @Schema(description = "题型：SINGLE/MULTI") String questionType,
    @Schema(description = "题干内容") String content,
    @Schema(description = "选项列表") List<OptionView> options) {
  public record OptionView(
      @Schema(description = "选项ID") long optionId,
      @Schema(description = "选项内容") String content,
      @Schema(description = "是否建议：0否 1是") int suggestFlag,
      @Schema(description = "改进建议（可为空）") String improvementTip,
      @Schema(description = "排序号（越小越靠前）") int sortNo,
      @Schema(description = "维度分列表") List<DimensionScore> dimensionScores) {}

  public record DimensionScore(
      @Schema(description = "维度编码") String dimensionCode,
      @Schema(description = "维度名称") String dimensionName,
      @Schema(description = "分值") int score) {}
}
