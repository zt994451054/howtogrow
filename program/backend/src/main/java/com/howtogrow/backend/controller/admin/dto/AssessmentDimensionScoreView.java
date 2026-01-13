package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自测记录维度得分")
public record AssessmentDimensionScoreView(
    @Schema(description = "维度编码") String dimensionCode,
    @Schema(description = "维度名称") String dimensionName,
    @Schema(description = "得分") long score) {}

