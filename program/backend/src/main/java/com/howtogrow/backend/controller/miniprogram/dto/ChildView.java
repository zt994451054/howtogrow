package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

@Schema(description = "孩子信息")
public record ChildView(
    @Schema(description = "孩子ID") long id,
    @Schema(description = "孩子昵称") String nickname,
    @Schema(description = "性别：0未知 1男 2女") int gender,
    @Schema(description = "出生日期（YYYY-MM-DD）") LocalDate birthDate) {}
