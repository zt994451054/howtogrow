package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "每日育儿日记提交/更新请求")
public record DailyParentingDiaryUpsertRequest(
    @Schema(description = "孩子ID") @NotNull Long childId,
    @Schema(description = "记录日期（YYYY-MM-DD；不传则默认今天）") LocalDate recordDate,
    @Schema(description = "日记内容（可为空；但 content/imageUrl 至少 1 个不为空）") String content,
    @Schema(description = "配图 URL（可选）") String imageUrl) {}
