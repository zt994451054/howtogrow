package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DimensionView(
    @Schema(description = "维度编码（唯一）") String code,
    @Schema(description = "维度名称") String name,
    @Schema(description = "排序号（越小越靠前）") int sortNo) {}
