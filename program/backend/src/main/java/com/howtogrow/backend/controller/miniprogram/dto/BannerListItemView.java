package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Banner 列表项（小程序）")
public record BannerListItemView(
    @Schema(description = "Banner ID") long id,
    @Schema(description = "标题") String title,
    @Schema(description = "封面图URL") String imageUrl,
    @Schema(description = "轮播顺序（越小越靠前）") int sortNo) {}

