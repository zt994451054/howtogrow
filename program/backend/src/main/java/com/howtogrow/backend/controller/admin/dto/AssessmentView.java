package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AssessmentView(
    @Schema(description = "自测ID") long assessmentId,
    @Schema(description = "用户ID") long userId,
    @Schema(description = "用户昵称") String userNickname,
    @Schema(description = "用户头像URL") String userAvatarUrl,
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "孩子昵称") String childNickname,
    @Schema(description = "提交日期（yyyy-MM-dd，北京时间口径）") LocalDate bizDate,
    @Schema(description = "提交时间") Instant submittedAt,
    @Schema(description = "情绪管理力得分") long emotionManagementScore,
    @Schema(description = "沟通表达力得分") long communicationExpressionScore,
    @Schema(description = "规则引导力得分") long ruleGuidanceScore,
    @Schema(description = "关系建设力得分") long relationshipBuildingScore,
    @Schema(description = "学习支持力得分") long learningSupportScore,
    @Schema(description = "维度得分（固定 5 维度）") List<AssessmentDimensionScoreView> dimensionScores) {}
