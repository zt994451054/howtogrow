package com.howtogrow.backend.controller.admin;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.auth.AdminPermissionRequired;
import com.howtogrow.backend.controller.admin.dto.AdminUserCreateRequest;
import com.howtogrow.backend.controller.admin.dto.AdminUserRoleUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.AdminUserView;
import com.howtogrow.backend.controller.admin.dto.PermissionView;
import com.howtogrow.backend.controller.admin.dto.RoleCreateRequest;
import com.howtogrow.backend.controller.admin.dto.RolePermissionUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.RoleView;
import com.howtogrow.backend.service.admin.AdminRbacService;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/rbac")
@AdminPermissionRequired({"RBAC:MANAGE"})
public class AdminRbacController {
  private final AdminRbacService rbacService;

  public AdminRbacController(AdminRbacService rbacService) {
    this.rbacService = rbacService;
  }

  @GetMapping("/permissions")
  public ApiResponse<List<PermissionView>> permissions() {
    return ApiResponse.ok(rbacService.listPermissions(), TraceId.current());
  }

  @GetMapping("/roles")
  public ApiResponse<List<RoleView>> roles() {
    return ApiResponse.ok(rbacService.listRoles(), TraceId.current());
  }

  @PostMapping("/roles")
  public ApiResponse<Void> createRole(@Valid @RequestBody RoleCreateRequest request) {
    rbacService.createRole(request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @PutMapping("/roles/{roleId}/permissions")
  public ApiResponse<Void> updateRolePermissions(
      @Parameter(description = "角色ID") @PathVariable long roleId,
      @Valid @RequestBody RolePermissionUpdateRequest request) {
    rbacService.updateRolePermissions(roleId, request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @GetMapping("/admin-users")
  public ApiResponse<List<AdminUserView>> adminUsers() {
    return ApiResponse.ok(rbacService.listAdminUsers(), TraceId.current());
  }

  @PostMapping("/admin-users")
  public ApiResponse<Void> createAdminUser(@Valid @RequestBody AdminUserCreateRequest request) {
    rbacService.createAdminUser(request);
    return ApiResponse.ok(null, TraceId.current());
  }

  @PutMapping("/admin-users/{adminUserId}/roles")
  public ApiResponse<Void> updateAdminUserRoles(
      @Parameter(description = "管理员ID") @PathVariable long adminUserId,
      @Valid @RequestBody AdminUserRoleUpdateRequest request) {
    rbacService.updateAdminUserRoles(adminUserId, request);
    return ApiResponse.ok(null, TraceId.current());
  }
}
