package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "烦恼场景（运营端）")
public record TroubleSceneView(
    @Schema(description = "场景ID") long id,
    @Schema(description = "名称") String name,
    @Schema(description = "logo图片URL") String logoUrl,
    @Schema(description = "适用最小年龄（整数，单位：岁，含边界）") int minAge,
    @Schema(description = "适用最大年龄（整数，单位：岁，含边界）") int maxAge,
    @Schema(description = "状态：1启用 0删除") int status,
    @Schema(description = "创建时间") Instant createdAt,
    @Schema(description = "更新时间") Instant updatedAt) {}
