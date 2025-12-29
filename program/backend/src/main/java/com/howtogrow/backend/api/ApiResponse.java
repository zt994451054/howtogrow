package com.howtogrow.backend.api;

import io.swagger.v3.oas.annotations.media.Schema;

public record ApiResponse<T>(
    @Schema(description = "业务码：OK/INVALID_REQUEST/...") String code,
    @Schema(description = "提示信息") String message,
    @Schema(description = "响应数据") T data,
    @Schema(description = "链路追踪ID（用于排查问题）") String traceId) {
  public static <T> ApiResponse<T> ok(T data, String traceId) {
    return new ApiResponse<>("OK", "ok", data, traceId);
  }

  public static ApiResponse<Void> error(String code, String message, String traceId) {
    return new ApiResponse<>(code, message, null, traceId);
  }
}
