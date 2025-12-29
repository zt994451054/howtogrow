package com.howtogrow.backend.api.exception;

import com.howtogrow.backend.api.ErrorCode;

public class AppException extends RuntimeException {
  private final ErrorCode code;

  public AppException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public ErrorCode code() {
    return code;
  }
}

