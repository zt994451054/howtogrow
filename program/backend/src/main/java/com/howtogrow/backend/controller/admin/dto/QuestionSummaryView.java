package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record QuestionSummaryView(
    @Schema(description = "题目ID") long questionId,
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") int minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") int maxAge,
    @Schema(description = "题型：SINGLE/MULTI") String questionType,
    @Schema(description = "状态：0禁用 1启用") int status,
    @Schema(description = "题干内容") String content,
    @Schema(description = "关联烦恼场景ID列表（可为空）") List<Long> troubleSceneIds) {}
