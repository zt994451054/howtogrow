package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Banner 详情（小程序）")
public record BannerDetailView(
    @Schema(description = "Banner ID") long id,
    @Schema(description = "标题") String title,
    @Schema(description = "富文本HTML") String htmlContent) {}

