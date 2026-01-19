package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "烦恼场景（小程序）")
public record TroubleSceneView(
    @Schema(description = "场景ID") long id,
    @Schema(description = "名称") String name,
    @Schema(description = "logo图片URL") String logoUrl,
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") int minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") int maxAge) {}
