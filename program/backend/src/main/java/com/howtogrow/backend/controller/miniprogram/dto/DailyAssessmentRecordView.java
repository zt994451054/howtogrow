package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "自测记录摘要（列表项）")
public record DailyAssessmentRecordView(
    @Schema(description = "自测记录ID") long assessmentId,
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "孩子昵称") String childName,
    @Schema(description = "提交时间（ISO-8601）") String submittedAt,
    @Schema(description = "AI 总结内容（可选；已生成则返回）") String aiSummary) {}
