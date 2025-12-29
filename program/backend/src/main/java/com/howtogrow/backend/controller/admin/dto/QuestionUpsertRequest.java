package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record QuestionUpsertRequest(
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") @NotNull @Min(0) @Max(18) Integer minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") @NotNull @Min(0) @Max(18) Integer maxAge,
    @Schema(description = "题型：SINGLE/MULTI") @NotBlank String questionType,
    @Schema(description = "题干内容") @NotBlank String content,
    @Schema(description = "状态：0禁用 1启用") @NotNull @Min(0) @Max(1) Integer status,
    @Schema(description = "选项列表") @NotEmpty @Valid List<OptionUpsert> options) {

  public record OptionUpsert(
      @Schema(description = "选项内容") @NotBlank String content,
      @Schema(description = "是否建议：0否 1是") @NotNull @Min(0) @Max(1) Integer suggestFlag,
      @Schema(description = "改进建议（可为空）") String improvementTip,
      @Schema(description = "排序号（可不传；后端按提交顺序生成）") Integer sortNo,
      @Schema(description = "维度分列表") @NotEmpty @Valid List<DimensionScore> dimensionScores) {}

  public record DimensionScore(
      @Schema(description = "维度编码") @NotBlank String dimensionCode,
      @Schema(description = "分值（>=1）") @NotNull @Min(1) Integer score) {}
}
