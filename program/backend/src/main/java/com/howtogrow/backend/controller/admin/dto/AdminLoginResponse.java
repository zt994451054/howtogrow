package com.howtogrow.backend.controller.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AdminLoginResponse(
    @Schema(description = "JWT Token") String token,
    @Schema(description = "Token 有效期（秒）") long expiresIn) {}
