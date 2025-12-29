package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.AdminUserCreateRequest;
import com.howtogrow.backend.controller.admin.dto.AdminUserRoleUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.AdminUserView;
import com.howtogrow.backend.controller.admin.dto.PermissionView;
import com.howtogrow.backend.controller.admin.dto.RoleCreateRequest;
import com.howtogrow.backend.controller.admin.dto.RolePermissionUpdateRequest;
import com.howtogrow.backend.controller.admin.dto.RoleView;
import com.howtogrow.backend.infrastructure.admin.AdminPermissionRepository;
import com.howtogrow.backend.infrastructure.admin.AdminRolePermissionRepository;
import com.howtogrow.backend.infrastructure.admin.AdminRoleRepository;
import com.howtogrow.backend.infrastructure.admin.AdminUserAdminRepository;
import com.howtogrow.backend.infrastructure.admin.AdminUserRoleRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRbacService {
  private final AdminPermissionRepository permissionRepo;
  private final AdminRoleRepository roleRepo;
  private final AdminRolePermissionRepository rolePermRepo;
  private final AdminUserAdminRepository adminUserRepo;
  private final AdminUserRoleRepository userRoleRepo;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AdminRbacService(
      AdminPermissionRepository permissionRepo,
      AdminRoleRepository roleRepo,
      AdminRolePermissionRepository rolePermRepo,
      AdminUserAdminRepository adminUserRepo,
      AdminUserRoleRepository userRoleRepo) {
    this.permissionRepo = permissionRepo;
    this.roleRepo = roleRepo;
    this.rolePermRepo = rolePermRepo;
    this.adminUserRepo = adminUserRepo;
    this.userRoleRepo = userRoleRepo;
  }

  public List<PermissionView> listPermissions() {
    return permissionRepo.listAll().stream()
        .map(p -> new PermissionView(p.id(), p.code(), p.name()))
        .toList();
  }

  public List<RoleView> listRoles() {
    return roleRepo.listAll().stream()
        .map(r -> new RoleView(r.id(), r.code(), r.name(), rolePermRepo.listPermissionCodes(r.id())))
        .toList();
  }

  @Transactional
  public void createRole(RoleCreateRequest request) {
    var code = normalizeCode(request.code());
    var name = request.name().trim();
    if (code.isBlank() || name.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "invalid role");
    }
    try {
      roleRepo.create(code, name);
    } catch (DuplicateKeyException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "role already exists");
    }
  }

  @Transactional
  public void updateRolePermissions(long roleId, RolePermissionUpdateRequest request) {
    var role =
        roleRepo
            .listAll()
            .stream()
            .filter(r -> r.id() == roleId)
            .findFirst()
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "role not found"));
    if ("SUPER_ADMIN".equalsIgnoreCase(role.code())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "SUPER_ADMIN permissions are managed by seed");
    }

    var codes =
        request.permissionCodes().stream()
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .distinct()
            .toList();
    var permissionIds = permissionRepo.listPermissionIdsByCodes(codes);
    if (permissionIds.size() != codes.size()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "some permissionCodes not found");
    }
    rolePermRepo.replaceRolePermissions(roleId, permissionIds);
  }

  public List<AdminUserView> listAdminUsers() {
    var users = adminUserRepo.listAll();
    return users.stream()
        .map(u -> new AdminUserView(u.id(), u.username(), u.status(), u.createdAt(), userRoleRepo.listRoleCodes(u.id())))
        .toList();
  }

  @Transactional
  public void createAdminUser(AdminUserCreateRequest request) {
    var username = request.username().trim();
    if (username.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "username is required");
    }
    if ("admin".equalsIgnoreCase(username)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "reserved username");
    }
    if (request.password().length() < 6) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "password too short");
    }
    var roles = resolveRoleIds(request.roleCodes());
    var hash = passwordEncoder.encode(request.password());
    long adminUserId;
    try {
      adminUserId = adminUserRepo.create(username, hash);
    } catch (DuplicateKeyException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "username already exists");
    }
    userRoleRepo.replaceUserRoles(adminUserId, roles);
  }

  @Transactional
  public void updateAdminUserRoles(long adminUserId, AdminUserRoleUpdateRequest request) {
    var target =
        adminUserRepo
            .listAll()
            .stream()
            .filter(u -> u.id() == adminUserId)
            .findFirst()
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "admin user not found"));
    if ("admin".equalsIgnoreCase(target.username())) {
      var requested = request.roleCodes().stream().map(this::normalizeCode).collect(Collectors.toSet());
      if (!requested.contains("SUPER_ADMIN")) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "cannot remove SUPER_ADMIN from admin");
      }
    }
    var roles = resolveRoleIds(request.roleCodes());
    userRoleRepo.replaceUserRoles(adminUserId, roles);
  }

  private List<Long> resolveRoleIds(List<String> roleCodes) {
    var codes =
        roleCodes == null
            ? List.<String>of()
            : roleCodes.stream().map(this::normalizeCode).filter(s -> !s.isBlank()).distinct().toList();
    if (codes.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "roleCodes is required");
    }
    var roles = roleRepo.listAll();
    Map<String, Long> byCode =
        roles.stream().collect(Collectors.toMap(r -> r.code().toUpperCase(Locale.ROOT), AdminRoleRepository.RoleRow::id));
    var ids =
        codes.stream()
            .map(c -> byCode.get(c.toUpperCase(Locale.ROOT)))
            .toList();
    if (ids.stream().anyMatch(java.util.Objects::isNull)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "some roleCodes not found");
    }
    return ids.stream().map(Long::longValue).toList();
  }

  private String normalizeCode(String code) {
    if (code == null) {
      return "";
    }
    return code.trim().toUpperCase(Locale.ROOT);
  }
}

