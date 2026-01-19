package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "每日烦恼记录")
public record DailyTroubleRecordView(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "记录日期") LocalDate recordDate,
    @Schema(description = "烦恼场景列表（仅未删除）") List<TroubleSceneView> scenes) {
  @Schema(description = "烦恼场景（小程序）")
  public record TroubleSceneView(
      @Schema(description = "场景ID") long id,
      @Schema(description = "名称") String name,
      @Schema(description = "logo图片URL") String logoUrl) {}
}

