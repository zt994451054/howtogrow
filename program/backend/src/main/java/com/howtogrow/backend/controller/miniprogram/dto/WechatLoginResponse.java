package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "微信登录响应")
public record WechatLoginResponse(
    @Schema(description = "JWT") String token,
    @Schema(description = "过期时间（秒）") long expiresIn,
    @Schema(description = "用户信息") MiniprogramUserView user) {}
