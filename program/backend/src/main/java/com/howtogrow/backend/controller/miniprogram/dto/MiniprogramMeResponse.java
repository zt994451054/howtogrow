package com.howtogrow.backend.controller.miniprogram.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "当前小程序用户信息响应")
public record MiniprogramMeResponse(@Schema(description = "用户") MiniprogramUserView user) {}
