package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record QuoteView(
    @Schema(description = "鸡汤语ID") long id,
    @Schema(description = "内容") String content,
    @Schema(description = "场景：每日觉察/育儿状态/烦恼档案/育儿日记") String scene,
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") int minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") int maxAge,
    @Schema(description = "状态：0禁用 1启用") int status) {}
