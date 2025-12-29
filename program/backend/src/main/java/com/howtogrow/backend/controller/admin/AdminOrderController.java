package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.admin.dto.OrderView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.service.admin.AdminOrderService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@com.howtogrow.backend.auth.AdminPermissionRequired({"ORDER:READ"})
public class AdminOrderController {
  private final AdminOrderService orderService;

  public AdminOrderController(AdminOrderService orderService) {
    this.orderService = orderService;
  }

  @GetMapping
  public ApiResponse<PageResponse<OrderView>> list(
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize) {
    return ApiResponse.ok(orderService.list(page, pageSize), TraceId.current());
  }
}
