package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "每日烦恼记录提交/更新请求")
public record DailyTroubleRecordUpsertRequest(
    @Schema(description = "孩子ID") @NotNull Long childId,
    @Schema(description = "记录日期（YYYY-MM-DD；不传则默认今天）") LocalDate recordDate,
    @Schema(description = "烦恼场景ID列表（至少1个）") @NotEmpty List<Long> sceneIds) {}

