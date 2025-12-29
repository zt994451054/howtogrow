package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.DimensionView;
import com.howtogrow.backend.service.admin.AdminDimensionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dimensions")
@com.howtogrow.backend.auth.AdminPermissionRequired({"QUESTION:MANAGE"})
public class AdminDimensionController {
  private final AdminDimensionService dimensionService;

  public AdminDimensionController(AdminDimensionService dimensionService) {
    this.dimensionService = dimensionService;
  }

  @GetMapping
  public ApiResponse<List<DimensionView>> list() {
    return ApiResponse.ok(dimensionService.list(), TraceId.current());
  }
}
