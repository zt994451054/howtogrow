package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

public record AssessmentView(
    @Schema(description = "自测ID") long assessmentId,
    @Schema(description = "用户ID") long userId,
    @Schema(description = "用户昵称") String userNickname,
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "孩子昵称") String childNickname,
    @Schema(description = "提交日期（yyyy-MM-dd，北京时间口径）") LocalDate bizDate,
    @Schema(description = "提交时间") Instant submittedAt) {}
