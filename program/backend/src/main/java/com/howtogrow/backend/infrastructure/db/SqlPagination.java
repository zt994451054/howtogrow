package com.howtogrow.backend.infrastructure.db;

public final class SqlPagination {
  private SqlPagination() {}

  public static String limit(int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
    return "LIMIT " + limit;
  }

  public static String limitOffset(int offset, int limit) {
    if (offset < 0) {
      throw new IllegalArgumentException("offset must be non-negative");
    }
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }
    return "LIMIT " + limit + " OFFSET " + offset;
  }
}
