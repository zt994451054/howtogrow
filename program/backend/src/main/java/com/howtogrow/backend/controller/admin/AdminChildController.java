package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.controller.admin.dto.AdminChildView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.service.admin.AdminChildService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/children")
@com.howtogrow.backend.auth.AdminPermissionRequired({"USER:READ"})
public class AdminChildController {
  private final AdminChildService childService;

  public AdminChildController(AdminChildService childService) {
    this.childService = childService;
  }

  @GetMapping
  public ApiResponse<PageResponse<AdminChildView>> list(
      @Parameter(description = "页码（从1开始）") @RequestParam(defaultValue = "1") @Min(1) int page,
      @Parameter(description = "每页条数（1-200）") @RequestParam(defaultValue = "20") @Min(1) @Max(200) int pageSize,
      @Parameter(description = "用户ID（可选）") @RequestParam(required = false) Long userId,
      @Parameter(description = "用户昵称关键词（可选）") @RequestParam(required = false) String userNickname,
      @Parameter(description = "孩子ID（可选）") @RequestParam(required = false) Long childId,
      @Parameter(description = "孩子昵称关键词（可选）") @RequestParam(required = false) String childNickname,
      @Parameter(description = "性别：0未知 1男 2女（可选）") @RequestParam(required = false) Integer gender,
      @Parameter(description = "年龄下限（岁，含边界，可选）") @RequestParam(required = false) @Min(0) @Max(18)
          Integer ageMin,
      @Parameter(description = "年龄上限（岁，含边界，可选）") @RequestParam(required = false) @Min(0) @Max(18)
          Integer ageMax,
      @Parameter(description = "状态：1启用 0删除（可选）") @RequestParam(required = false) Integer status) {
    return ApiResponse.ok(
        childService.list(page, pageSize, userId, userNickname, childId, childNickname, gender, ageMin, ageMax, status),
        TraceId.current());
  }
}
