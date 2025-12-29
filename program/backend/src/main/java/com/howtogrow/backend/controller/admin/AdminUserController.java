package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.UserView;
import com.howtogrow.backend.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@com.howtogrow.backend.auth.AdminPermissionRequired({"USER:READ"})
public class AdminUserController {
  private final AdminUserService userService;

  public AdminUserController(AdminUserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public ApiResponse<PageResponse<UserView>> list(
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize) {
    return ApiResponse.ok(userService.list(page, pageSize), TraceId.current());
  }
}
