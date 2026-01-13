package com.howtogrow.backend.auth;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.config.AdminSecurityProperties;
import com.howtogrow.backend.service.admin.AdminPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminPermissionInterceptor implements HandlerInterceptor {
  private static final String REQ_ATTR_PERMS = "adminPermissionCodes";

  private final AdminPermissionService permissionService;
  private final AdminSecurityProperties adminSecurityProperties;

  public AdminPermissionInterceptor(
      AdminPermissionService permissionService, AdminSecurityProperties adminSecurityProperties) {
    this.permissionService = permissionService;
    this.adminSecurityProperties = adminSecurityProperties;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod hm)) {
      return true;
    }
    if (!adminSecurityProperties.enforcePermissionChecks()) {
      return true;
    }
    var required = findRequired(hm);
    if (required == null || required.length == 0) {
      return true;
    }

    var admin = AuthContext.requireAdmin();
    @SuppressWarnings("unchecked")
    Set<String> perms = (Set<String>) request.getAttribute(REQ_ATTR_PERMS);
    if (perms == null) {
      perms = permissionService.permissionsFor(admin.userId());
      request.setAttribute(REQ_ATTR_PERMS, perms);
    }

    for (var code : required) {
      if (code != null && !code.isBlank() && perms.contains(code)) {
        return true;
      }
    }
    throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
  }

  private static String[] findRequired(HandlerMethod hm) {
    var methodAnn = hm.getMethodAnnotation(AdminPermissionRequired.class);
    if (methodAnn != null) {
      return methodAnn.value();
    }
    var typeAnn = hm.getBeanType().getAnnotation(AdminPermissionRequired.class);
    if (typeAnn != null) {
      return typeAnn.value();
    }
    return null;
  }
}
