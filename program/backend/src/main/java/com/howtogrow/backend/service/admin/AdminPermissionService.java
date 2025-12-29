package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.infrastructure.admin.AdminRbacRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AdminPermissionService {
  private final AdminRbacRepository rbacRepo;

  public AdminPermissionService(AdminRbacRepository rbacRepo) {
    this.rbacRepo = rbacRepo;
  }

  public Set<String> permissionsFor(long adminUserId) {
    return new HashSet<>(rbacRepo.listPermissionCodes(adminUserId));
  }
}

