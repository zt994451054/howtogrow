package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "每日育儿日记")
public record DailyParentingDiaryView(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "记录日期（YYYY-MM-DD）") LocalDate recordDate,
    @Schema(description = "日记内容") String content,
    @Schema(description = "配图 URL（可选）") String imageUrl) {}

