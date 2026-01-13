package com.howtogrow.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.TraceId;
import com.howtogrow.backend.api.exception.AppException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final ObjectMapper objectMapper;

  public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
    this.jwtService = jwtService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilterAsyncDispatch() {
    // SSE uses async dispatch; without this, the JWT auth context is lost and Spring Security may deny the async dispatch
    // after the response is already committed, causing "Unable to handle ... because the response is already committed."
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    var auth = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (auth != null && auth.startsWith("Bearer ")) {
      var token = auth.substring("Bearer ".length()).trim();
      if (!token.isBlank()) {
        try {
          var user = jwtService.verify(token);
          var authentication = new UsernamePasswordAuthenticationToken(user, token, List.of());
          SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AppException e) {
          if (e.code() != ErrorCode.UNAUTHORIZED) {
            throw e;
          }
          writeUnauthorized(response, e.getMessage());
          return;
        } catch (RuntimeException e) {
          writeUnauthorized(response, "登录已失效，请重新登录");
          return;
        }
      }
    }
    filterChain.doFilter(request, response);
  }

  private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
    var traceId = TraceId.current();
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setHeader(TraceId.HEADER, traceId);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error("UNAUTHORIZED", message, traceId)));
  }
}
