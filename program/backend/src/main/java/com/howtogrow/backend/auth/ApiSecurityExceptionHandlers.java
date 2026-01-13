package com.howtogrow.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.TraceId;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityExceptionHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final ObjectMapper objectMapper;

  public ApiSecurityExceptionHandlers(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      org.springframework.security.core.AuthenticationException authException)
      throws IOException, ServletException {
    write(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED.name(), "未登录");
  }

  @Override
  public void handle(
      HttpServletRequest request,
      HttpServletResponse response,
      AccessDeniedException accessDeniedException)
      throws IOException, ServletException {
    write(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN_RESOURCE.name(), "无权限");
  }

  private void write(HttpServletResponse response, int status, String code, String message)
      throws IOException {
    if (response.isCommitted()) {
      return;
    }
    var traceId = TraceId.current();
    response.resetBuffer();
    response.setStatus(status);
    response.setHeader(TraceId.HEADER, traceId);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message, traceId)));
  }
}
