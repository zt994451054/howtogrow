package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.auth.Audience;
import com.howtogrow.backend.auth.JwtProperties;
import com.howtogrow.backend.auth.JwtService;
import com.howtogrow.backend.controller.admin.dto.AdminLoginResponse;
import com.howtogrow.backend.controller.admin.dto.AdminMeResponse;
import com.howtogrow.backend.infrastructure.admin.AdminRbacRepository;
import com.howtogrow.backend.infrastructure.admin.AdminUserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminAuthService {
  private final AdminUserRepository adminUserRepo;
  private final AdminRbacRepository rbacRepo;
  private final JwtService jwtService;
  private final JwtProperties jwtProperties;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AdminAuthService(
      AdminUserRepository adminUserRepo,
      AdminRbacRepository rbacRepo,
      JwtService jwtService,
      JwtProperties jwtProperties) {
    this.adminUserRepo = adminUserRepo;
    this.rbacRepo = rbacRepo;
    this.jwtService = jwtService;
    this.jwtProperties = jwtProperties;
  }

  public AdminLoginResponse login(String username, String password) {
    var admin =
        adminUserRepo
            .findByUsername(username.trim())
            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "invalid credentials"));
    if (admin.status() != 1) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "admin disabled");
    }
    if (!passwordEncoder.matches(password, admin.passwordHash())) {
      throw new AppException(ErrorCode.UNAUTHORIZED, "invalid credentials");
    }
    var token = jwtService.issue(Audience.ADMIN, admin.id());
    return new AdminLoginResponse(token, jwtProperties.ttlSeconds());
  }

  public AdminMeResponse me(long adminUserId) {
    var admin =
        adminUserRepo.findById(adminUserId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "admin not found"));
    var perms = rbacRepo.listPermissionCodes(adminUserId);
    return new AdminMeResponse(admin.id(), admin.username(), perms);
  }
}

