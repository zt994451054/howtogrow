package com.howtogrow.backend.infrastructure.admin;

import com.howtogrow.backend.infrastructure.db.SqlPagination;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class QuoteAdminRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuoteAdminRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countQuotes(String scene, Integer status, String keyword) {
    var params = new MapSqlParameterSource();
    var sql = new StringBuilder("SELECT COUNT(*) FROM quote WHERE deleted_at IS NULL");
    appendWhere(sql, params, scene, status, keyword);
    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count == null ? 0L : count;
  }

  public List<QuoteRow> listQuotes(int offset, int limit, String scene, Integer status, String keyword) {
    var params = new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    var sql =
        new StringBuilder(
            """
            SELECT id, content, scene, min_age, max_age, status
            FROM quote
            WHERE deleted_at IS NULL
            """);
    appendWhere(sql, params, scene, status, keyword);
    sql.append("\nORDER BY id DESC\n").append(SqlPagination.limitOffset(offset, limit));
    return jdbc.query(
        sql.toString(),
        params,
        (rs, rowNum) ->
            new QuoteRow(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getString("scene"),
                rs.getInt("min_age"),
                rs.getInt("max_age"),
                rs.getInt("status")));
  }

  private static void appendWhere(StringBuilder sql, MapSqlParameterSource params, String scene, Integer status, String keyword) {
    if (scene != null) {
      sql.append("\n  AND scene = :scene");
      params.addValue("scene", scene);
    }
    if (status != null) {
      sql.append("\n  AND status = :status");
      params.addValue("status", status);
    }
    if (keyword != null) {
      sql.append("\n  AND content LIKE :keyword");
      params.addValue("keyword", "%" + keyword + "%");
    }
  }

  public List<QuoteRow> listAll() {
    var sql =
        """
        SELECT id, content, scene, min_age, max_age, status
        FROM quote
        WHERE deleted_at IS NULL
        ORDER BY id DESC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new QuoteRow(
                rs.getLong("id"),
                rs.getString("content"),
                rs.getString("scene"),
                rs.getInt("min_age"),
                rs.getInt("max_age"),
                rs.getInt("status")));
  }

  public long create(String content, String scene, int minAge, int maxAge, int status) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO quote(content, scene, min_age, max_age, status, created_at, updated_at)
        VALUES (:content, :scene, :minAge, :maxAge, :status, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("content", content)
            .addValue("scene", scene)
            .addValue("minAge", minAge)
            .addValue("maxAge", maxAge)
            .addValue("status", status),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create quote");
    }
    return id.longValue();
  }

  public void update(long id, String content, String scene, int minAge, int maxAge, int status) {
    var sql =
        """
        UPDATE quote
        SET content = :content, scene = :scene, min_age = :minAge, max_age = :maxAge, status = :status, updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(
        sql, Map.of("id", id, "content", content, "scene", scene, "minAge", minAge, "maxAge", maxAge, "status", status));
  }

  public void softDelete(long id) {
    var sql =
        """
        UPDATE quote
        SET status = 0, deleted_at = NOW(3), updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id));
  }

  public record QuoteRow(long id, String content, String scene, int minAge, int maxAge, int status) {}
}
