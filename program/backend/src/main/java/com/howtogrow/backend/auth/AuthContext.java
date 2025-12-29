package com.howtogrow.backend.auth;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthContext {
  private AuthContext() {}

  public static Optional<AuthUser> currentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    if (authentication.getPrincipal() instanceof AuthUser user) {
      return Optional.of(user);
    }
    return Optional.empty();
  }

  public static AuthUser requireMiniprogram() {
    var user = currentUser().orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "unauthorized"));
    if (user.audience() != Audience.MINIPROGRAM) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "forbidden");
    }
    return user;
  }

  public static AuthUser requireAdmin() {
    var user = currentUser().orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "unauthorized"));
    if (user.audience() != Audience.ADMIN) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "forbidden");
    }
    return user;
  }
}

