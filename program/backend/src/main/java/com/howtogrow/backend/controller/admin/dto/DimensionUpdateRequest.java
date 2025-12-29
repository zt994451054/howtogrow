package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DimensionUpdateRequest(
    @Schema(description = "维度编码（唯一）") @NotBlank String code,
    @Schema(description = "维度名称") @NotBlank String name,
    @Schema(description = "维度描述") String description,
    @Schema(description = "状态：0禁用 1启用") @NotNull @Min(0) @Max(1) Integer status,
    @Schema(description = "排序号（越小越靠前）") @NotNull Integer sortNo) {}
