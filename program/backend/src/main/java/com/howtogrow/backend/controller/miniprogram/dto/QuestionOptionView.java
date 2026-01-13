package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "题目选项")
public record QuestionOptionView(
    @Schema(description = "选项ID") long optionId,
    @Schema(description = "选项内容") String content,
    @Schema(description = "排序号（升序）") int sortNo,
    @Schema(description = "建议属性：1建议 0不建议") int suggestFlag,
    @Schema(description = "改进建议文案（可选）") String improvementTip) {}
