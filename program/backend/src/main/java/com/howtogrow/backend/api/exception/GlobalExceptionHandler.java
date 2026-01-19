package com.howtogrow.backend.api.exception;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.TraceId;
import java.sql.SQLException;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(AppException.class)
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleApp(
      AppException e, HttpServletRequest request) {
    var status =
        e.code() == ErrorCode.UNAUTHORIZED
            ? HttpStatus.UNAUTHORIZED
            : e.code() == ErrorCode.FORBIDDEN_RESOURCE ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
    if (e.code() == ErrorCode.RATE_LIMITED) {
      status = HttpStatus.TOO_MANY_REQUESTS;
    }
    log.warn(
        "Handled AppException: code={} status={} traceId={} method={} path={} message={}",
        e.code().name(),
        status.value(),
        TraceId.current(),
        request.getMethod(),
        request.getRequestURI(),
        e.getMessage());
    var body = ApiResponse.error(e.code().name(), e.getMessage(), TraceId.current());
    return org.springframework.http.ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleValidation(
      Exception e, HttpServletRequest request) {
    log.warn(
        "Validation failed: traceId={} method={} path={} error={}",
        TraceId.current(),
        request.getMethod(),
        request.getRequestURI(),
        e.getClass().getSimpleName());
    var body =
        ApiResponse.error(ErrorCode.INVALID_REQUEST.name(), "请求参数错误", TraceId.current());
    return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ErrorResponseException.class)
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleErrorResponseException(
      ErrorResponseException e, HttpServletRequest request) {
    var status = e.getStatusCode().value();
    var code =
        status == 401
            ? ErrorCode.UNAUTHORIZED.name()
            : status == 403 ? ErrorCode.FORBIDDEN_RESOURCE.name() : ErrorCode.INVALID_REQUEST.name();
    var body = e.getBody();
    var message = body == null ? e.getMessage() : body.getDetail();
    if (status == 401) message = "未登录";
    if (status == 403) message = "无权限";
    if (status == 404) message = "资源不存在";
    log.warn(
        "Handled ErrorResponseException: httpStatus={} code={} traceId={} method={} path={} message={}",
        status,
        code,
        TraceId.current(),
        request.getMethod(),
        request.getRequestURI(),
        message);
    var response =
        ApiResponse.error(code, message == null ? "请求失败" : message, TraceId.current());
    return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(response);
  }

  @ExceptionHandler(DataAccessException.class)
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleDataAccess(
      DataAccessException e, HttpServletRequest request) {
    var sqlException = findCause(e, SQLException.class);
    if (sqlException != null && sqlException.getErrorCode() == 3819) {
      var constraintName = parseCheckConstraintName(sqlException.getMessage());
      var message =
          switch (constraintName) {
            case "ck_quote_scene" -> "场景不合法（允许：每日觉察/育儿状态/烦恼档案/育儿日记）";
            case "ck_quote_age_range" -> "年龄范围不合法（0-18 且 minAge<=maxAge）";
            case "ck_child_parent_identity" -> "家长身份不合法（允许：爸爸/妈妈/奶奶/爷爷/外公/外婆）";
            default -> "数据不合法";
          };
      log.warn(
          "Data constraint violated: constraint={} traceId={} method={} path={}",
          constraintName == null ? "unknown" : constraintName,
          TraceId.current(),
          request.getMethod(),
          request.getRequestURI());
      var body = ApiResponse.error(ErrorCode.INVALID_REQUEST.name(), message, TraceId.current());
      return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    log.error(
        "Unhandled DataAccessException: traceId={} method={} path={}",
        TraceId.current(),
        request.getMethod(),
        request.getRequestURI(),
        e);
    var body = ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), "服务异常", TraceId.current());
    return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  @ExceptionHandler(Exception.class)
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleUnexpected(
      Exception e, HttpServletRequest request) {
    log.error(
        "Unhandled error: traceId={} method={} path={}",
        TraceId.current(),
        request.getMethod(),
        request.getRequestURI(),
        e);
    var body =
        ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), "服务异常", TraceId.current());
    return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private static String parseCheckConstraintName(String message) {
    if (message == null || message.isBlank()) {
      return null;
    }
    // MySQL: "Check constraint 'ck_xxx' is violated."
    var marker = "constraint '";
    var start = message.indexOf(marker);
    if (start < 0) {
      return null;
    }
    start += marker.length();
    var end = message.indexOf('\'', start);
    if (end <= start) {
      return null;
    }
    return message.substring(start, end);
  }

  private static <T extends Throwable> T findCause(Throwable e, Class<T> type) {
    var cur = e;
    while (cur != null) {
      if (type.isInstance(cur)) {
        return type.cast(cur);
      }
      cur = cur.getCause();
    }
    return null;
  }
}
