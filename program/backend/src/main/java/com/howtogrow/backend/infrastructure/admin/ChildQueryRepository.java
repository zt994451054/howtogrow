package com.howtogrow.backend.infrastructure.admin;

import com.howtogrow.backend.infrastructure.db.SqlPagination;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChildQueryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public ChildQueryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countChildren(ChildQuery query) {
    var built = buildBaseSql(query);
    var sql = "SELECT COUNT(*) " + built.fromWhereSql();
    Long count = jdbc.queryForObject(sql, built.params(), Long.class);
    return count == null ? 0L : count;
  }

  public List<ChildRow> listChildren(ChildQuery query, int offset, int limit) {
    var built = buildBaseSql(query);
    var sql =
        """
        SELECT
          c.id AS child_id,
          c.user_id,
          u.nickname AS user_nickname,
          u.avatar_url AS user_avatar_url,
          c.nickname AS child_nickname,
          c.gender,
          c.birth_date,
          c.status,
          c.created_at
        """
            + built.fromWhereSql()
            + " ORDER BY c.id DESC "
            + SqlPagination.limitOffset(offset, limit);
    return jdbc.query(
        sql,
        built.params(),
        (rs, rowNum) -> {
          Instant createdAt = null;
          var ts = rs.getTimestamp("created_at");
          if (ts != null) createdAt = ts.toInstant();
          return new ChildRow(
              rs.getLong("child_id"),
              rs.getLong("user_id"),
              rs.getString("user_nickname"),
              rs.getString("user_avatar_url"),
              rs.getString("child_nickname"),
              rs.getInt("gender"),
              rs.getObject("birth_date", LocalDate.class),
              rs.getInt("status"),
              createdAt);
        });
  }

  private static BuiltSql buildBaseSql(ChildQuery query) {
    var where = new ArrayList<String>();
    var params = new HashMap<String, Object>();

    String base =
        """
        FROM child c
        JOIN user_account u ON u.id = c.user_id
        WHERE 1=1
        """;

    if (query != null) {
      if (query.userId() != null && query.userId() > 0) {
        where.add("c.user_id = :userId");
        params.put("userId", query.userId());
      }
      if (query.childId() != null && query.childId() > 0) {
        where.add("c.id = :childId");
        params.put("childId", query.childId());
      }
      if (query.gender() != null && query.gender() >= 0) {
        where.add("c.gender = :gender");
        params.put("gender", query.gender());
      }
      if (query.status() != null && (query.status() == 0 || query.status() == 1)) {
        where.add("c.status = :status");
        params.put("status", query.status());
      }
      if (query.userNickname() != null && !query.userNickname().isBlank()) {
        where.add("u.nickname LIKE :userNickname");
        params.put("userNickname", "%" + query.userNickname().trim() + "%");
      }
      if (query.childNickname() != null && !query.childNickname().isBlank()) {
        where.add("c.nickname LIKE :childNickname");
        params.put("childNickname", "%" + query.childNickname().trim() + "%");
      }
    }

    var sb = new StringBuilder(base);
    for (var w : where) {
      sb.append(" AND ").append(w);
    }
    return new BuiltSql(sb.toString(), params);
  }

  private record BuiltSql(String fromWhereSql, Map<String, Object> params) {}

  public record ChildQuery(
      Long userId,
      String userNickname,
      Long childId,
      String childNickname,
      Integer gender,
      Integer status) {}

  public record ChildRow(
      long childId,
      long userId,
      String userNickname,
      String userAvatarUrl,
      String childNickname,
      int gender,
      LocalDate birthDate,
      int status,
      Instant createdAt) {}
}

