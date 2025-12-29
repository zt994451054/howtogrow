package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AuthContext;
import com.howtogrow.backend.controller.admin.dto.PlanCreateRequest;
import com.howtogrow.backend.controller.admin.dto.PlanUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.PlanView;
import com.howtogrow.backend.service.admin.AdminPlanService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/plans")
@com.howtogrow.backend.auth.AdminPermissionRequired({"PLAN:MANAGE"})
public class AdminPlanController {
  private final AdminPlanService planService;

  public AdminPlanController(AdminPlanService planService) {
    this.planService = planService;
  }

  @GetMapping
  public ApiResponse<List<PlanView>> list() {
    return ApiResponse.ok(planService.list(), TraceId.current());
  }

  @PostMapping
  public ApiResponse<Void> create(@Valid @RequestBody PlanCreateRequest request) {
    planService.create(request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @PutMapping("/{id}")
  public ApiResponse<Void> update(
      @Parameter(description = "套餐ID") @PathVariable long id,
      @Valid @RequestBody PlanUpdateRequest request) {
    planService.update(id, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@Parameter(description = "套餐ID") @PathVariable long id) {
    planService.delete(id);
    return ApiResponse.ok(null, TraceId.current());
  }
}
