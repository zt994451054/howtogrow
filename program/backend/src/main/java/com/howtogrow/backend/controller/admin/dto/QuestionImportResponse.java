package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record QuestionImportResponse(
    @Schema(description = "总行数") int total,
    @Schema(description = "成功数") int success,
    @Schema(description = "失败数") int failed,
    @Schema(description = "失败明细") List<Failure> failures) {
  public record Failure(
      @Schema(description = "行号（Excel 中的行号）") int row,
      @Schema(description = "失败原因") String reason) {}
}
