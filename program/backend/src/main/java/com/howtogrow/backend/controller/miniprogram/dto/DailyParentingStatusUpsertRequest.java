package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "每日育儿状态提交/更新请求")
public record DailyParentingStatusUpsertRequest(
    @Schema(description = "孩子ID") @NotNull Long childId,
    @Schema(description = "记录日期（YYYY-MM-DD；不传则默认今天）") LocalDate recordDate,
    @Schema(description = "育儿状态：失望/平静/乐观/难过/无奈/愤怒/欣慰/担忧/开心/绝望") @NotBlank String statusCode) {}

