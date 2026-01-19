package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "坚持进步（觉察）统计：从首次记录到今日的天数（含首日/今日，最小为1）")
public record AwarenessPersistenceResponse(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "首次记录日期（YYYY-MM-DD；若无任何记录则为今日）") LocalDate firstRecordDate,
    @Schema(description = "今日（YYYY-MM-DD，中国时区口径）") LocalDate today,
    @Schema(description = "坚持进步天数（含首日/今日，最小为1）") long persistenceDays) {}

