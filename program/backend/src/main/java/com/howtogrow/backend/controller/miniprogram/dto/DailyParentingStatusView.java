package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "每日育儿状态")
public record DailyParentingStatusView(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "记录日期") LocalDate recordDate,
    @Schema(description = "育儿状态") String statusCode) {}

