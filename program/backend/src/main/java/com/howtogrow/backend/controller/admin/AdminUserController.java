package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.controller.admin.dto.SubscriptionExtendRequest;
import com.howtogrow.backend.controller.admin.dto.SubscriptionExtendResponse;
import com.howtogrow.backend.controller.admin.dto.UserView;
import com.howtogrow.backend.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize,
      @Parameter(description = "用户ID（可选）") @RequestParam(required = false) Long userId,
      @Parameter(description = "昵称/openid 关键词（可选）") @RequestParam(required = false) String keyword,
      @Parameter(description = "免费体验：true/false（可选）") @RequestParam(required = false) Boolean freeTrialUsed,
      @Parameter(description = "订阅状态：ACTIVE/EXPIRED/NONE（可选）") @RequestParam(required = false)
          String subscriptionStatus) {
    return ApiResponse.ok(
        userService.list(page, pageSize, userId, keyword, freeTrialUsed, subscriptionStatus),
        TraceId.current());
  }

  @PostMapping("/{userId}/subscription/extend")
  public ApiResponse<SubscriptionExtendResponse> extendSubscription(
      @Parameter(description = "用户ID") @PathVariable long userId,
      @Valid @RequestBody SubscriptionExtendRequest request) {
    var endAt = userService.extendSubscription(userId, request.days());
    return ApiResponse.ok(new SubscriptionExtendResponse(userId, endAt), TraceId.current());
  }
}
