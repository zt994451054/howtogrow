package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "自测记录详情")
public record DailyAssessmentRecordDetailResponse(
    @Schema(description = "自测记录ID") long assessmentId,
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "孩子昵称") String childName,
    @Schema(description = "提交时间（ISO-8601）") String submittedAt,
    @Schema(description = "AI 总结内容（可选；已生成则返回）") String aiSummary,
    @Schema(description = "题目列表（固定 5 题）") List<DailyAssessmentItemView> items,
    @Schema(description = "作答列表（按题汇总）") List<DailyAssessmentAnswerView> answers) {}
