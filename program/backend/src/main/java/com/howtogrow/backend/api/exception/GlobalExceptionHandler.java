package com.howtogrow.backend.api.exception;

import com.howtogrow.backend.api.ApiResponse;
import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.TraceId;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleApp(AppException e) {
    var status =
        e.code() == ErrorCode.UNAUTHORIZED
            ? HttpStatus.UNAUTHORIZED
            : e.code() == ErrorCode.FORBIDDEN_RESOURCE ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
    if (e.code() == ErrorCode.RATE_LIMITED) {
      status = HttpStatus.TOO_MANY_REQUESTS;
    }
    var body = ApiResponse.error(e.code().name(), e.getMessage(), TraceId.current());
    return org.springframework.http.ResponseEntity.status(status).body(body);
  }

  @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleValidation(Exception e) {
    var body =
        ApiResponse.error(ErrorCode.INVALID_REQUEST.name(), "invalid request", TraceId.current());
    return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ErrorResponseException.class)
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleErrorResponseException(
      ErrorResponseException e) {
    var status = e.getStatusCode().value();
    var code =
        status == 401
            ? ErrorCode.UNAUTHORIZED.name()
            : status == 403 ? ErrorCode.FORBIDDEN_RESOURCE.name() : ErrorCode.INVALID_REQUEST.name();
    var body = e.getBody();
    var message = body == null ? e.getMessage() : body.getDetail();
    var response =
        ApiResponse.error(code, message == null ? "error" : message, TraceId.current());
    return org.springframework.http.ResponseEntity.status(e.getStatusCode()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
    log.error("Unhandled error", e);
    var body =
        ApiResponse.error(ErrorCode.INTERNAL_ERROR.name(), "internal error", TraceId.current());
    return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }
}
