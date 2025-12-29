package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "微信登录请求")
public record WechatLoginRequest(@Schema(description = "wx.login() 返回的 code；dev 可使用 mock:<openid>") @NotBlank String code) {}
