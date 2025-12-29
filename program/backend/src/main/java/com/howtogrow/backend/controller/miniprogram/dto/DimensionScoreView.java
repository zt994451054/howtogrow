package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "维度得分")
public record DimensionScoreView(
    @Schema(description = "维度编码") String dimensionCode,
    @Schema(description = "维度名称") String dimensionName,
    @Schema(description = "得分") long score) {}
