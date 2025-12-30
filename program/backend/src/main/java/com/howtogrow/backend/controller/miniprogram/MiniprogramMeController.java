package com.howtogrow.backend.controller.miniprogram;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.miniprogram.dto.MiniprogramMeResponse;
import com.howtogrow.backend.controller.miniprogram.dto.UpdateProfileRequest;
import com.howtogrow.backend.service.miniprogram.MiniprogramMeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/miniprogram")
public class MiniprogramMeController {
  private final MiniprogramMeService meService;

  public MiniprogramMeController(MiniprogramMeService meService) {
    this.meService = meService;
  }

  @GetMapping("/me")
  public ApiResponse<MiniprogramMeResponse> me() {
    var user = AuthContext.requireMiniprogram();
    return ApiResponse.ok(meService.getMe(user.userId()), TraceId.current());
  }

  @PostMapping("/me/profile")
  public ApiResponse<Void> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
    var user = AuthContext.requireMiniprogram();
    meService.updateProfile(user.userId(), request.nickname(), request.avatarUrl());
    return ApiResponse.ok(null, TraceId.current());
  }
}
