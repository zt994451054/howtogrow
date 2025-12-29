package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.admin.dto.AdminLoginRequest;
import com.howtogrow.backend.controller.admin.dto.AdminLoginResponse;
import com.howtogrow.backend.controller.admin.dto.AdminMeResponse;
import com.howtogrow.backend.service.admin.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {
  private final AdminAuthService authService;

  public AdminAuthController(AdminAuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
    return ApiResponse.ok(authService.login(request.username(), request.password()), TraceId.current());
  }

  @GetMapping("/me")
  public ApiResponse<AdminMeResponse> me() {
    var admin = AuthContext.requireAdmin();
    return ApiResponse.ok(authService.me(admin.userId()), TraceId.current());
  }
}

