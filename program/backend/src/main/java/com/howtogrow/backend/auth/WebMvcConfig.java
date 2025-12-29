package com.howtogrow.backend.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  private final AdminPermissionInterceptor adminPermissionInterceptor;

  public WebMvcConfig(AdminPermissionInterceptor adminPermissionInterceptor) {
    this.adminPermissionInterceptor = adminPermissionInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(adminPermissionInterceptor)
        .addPathPatterns("/api/v1/admin/**")
        .excludePathPatterns("/api/v1/admin/auth/**");
  }
}

