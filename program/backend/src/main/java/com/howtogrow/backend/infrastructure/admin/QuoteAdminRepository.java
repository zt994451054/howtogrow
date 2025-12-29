package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class QuoteAdminRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuoteAdminRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<QuoteRow> listAll() {
    var sql =
        """
        SELECT id, content, status
        FROM quote
        WHERE deleted_at IS NULL
        ORDER BY id DESC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) -> new QuoteRow(rs.getLong("id"), rs.getString("content"), rs.getInt("status")));
  }

  public long create(String content, int status) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO quote(content, status, created_at, updated_at)
        VALUES (:content, :status, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("content", content)
            .addValue("status", status),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create quote");
    }
    return id.longValue();
  }

  public void update(long id, String content, int status) {
    var sql =
        """
        UPDATE quote
        SET content = :content, status = :status, updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id, "content", content, "status", status));
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

  public record QuoteRow(long id, String content, int status) {}
}

