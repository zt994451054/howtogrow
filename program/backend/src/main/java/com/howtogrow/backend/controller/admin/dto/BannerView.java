package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Banner（运营端）")
public record BannerView(
    @Schema(description = "Banner ID") long id,
    @Schema(description = "标题") String title,
    @Schema(description = "封面图URL") String imageUrl,
    @Schema(description = "富文本HTML") String htmlContent,
    @Schema(description = "状态：1上架 0下架") int status,
    @Schema(description = "轮播顺序（越小越靠前）") int sortNo,
    @Schema(description = "创建时间") Instant createdAt,
    @Schema(description = "更新时间") Instant updatedAt) {}

