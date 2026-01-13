package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "孩子（运营端列表项）")
public record AdminChildView(
    @Schema(description = "孩子ID") long childId,
    @Schema(description = "所属用户ID") long userId,
    @Schema(description = "用户昵称") String userNickname,
    @Schema(description = "用户头像URL") String userAvatarUrl,
    @Schema(description = "孩子昵称") String childNickname,
    @Schema(description = "性别：0未知 1男 2女") int gender,
    @Schema(description = "出生日期（YYYY-MM-DD）") LocalDate birthDate,
    @Schema(description = "状态：1启用 0删除") int status,
    @Schema(description = "创建时间") Instant createdAt) {}

