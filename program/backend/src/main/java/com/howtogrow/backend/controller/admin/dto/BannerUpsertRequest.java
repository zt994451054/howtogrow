package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Banner 新增/更新请求")
public record BannerUpsertRequest(
    @Schema(description = "标题") @NotBlank String title,
    @Schema(description = "封面图URL") @NotBlank String imageUrl,
    @Schema(description = "富文本HTML") @NotBlank String htmlContent,
    @Schema(description = "状态：1上架 0下架") @NotNull @Min(0) @Max(1) Integer status,
    @Schema(description = "轮播顺序（越小越靠前）") @NotNull Integer sortNo) {}

