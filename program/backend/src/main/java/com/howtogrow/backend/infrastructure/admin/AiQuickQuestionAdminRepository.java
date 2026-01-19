package com.howtogrow.backend.infrastructure.admin;

import com.howtogrow.backend.infrastructure.db.SqlPagination;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AiQuickQuestionAdminRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AiQuickQuestionAdminRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long count(String keyword, Integer status) {
    var params = new MapSqlParameterSource();
    var sql = new StringBuilder("SELECT COUNT(*) FROM ai_agent_quick_question WHERE deleted_at IS NULL");
    appendWhere(sql, params, keyword, status);
    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count == null ? 0L : count;
  }

  public java.util.List<Row> listPage(int offset, int limit, String keyword, Integer status) {
    var params = new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    var sql =
        new StringBuilder(
            """
            SELECT id, prompt, status, sort_no, created_at, updated_at
            FROM ai_agent_quick_question
            WHERE deleted_at IS NULL
            """);
    appendWhere(sql, params, keyword, status);
    sql.append("\nORDER BY sort_no ASC, id DESC\n").append(SqlPagination.limitOffset(offset, limit));
    return jdbc.query(
        sql.toString(),
        params,
        (rs, rowNum) ->
            new Row(
                rs.getLong("id"),
                rs.getString("prompt"),
                rs.getInt("status"),
                rs.getInt("sort_no"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()));
  }

  public Optional<Row> findById(long id) {
    var sql =
        """
        SELECT id, prompt, status, sort_no, created_at, updated_at
        FROM ai_agent_quick_question
        WHERE id = :id AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("id", id),
            (rs, rowNum) ->
                new Row(
                    rs.getLong("id"),
                    rs.getString("prompt"),
                    rs.getInt("status"),
                    rs.getInt("sort_no"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
    return rows.stream().findFirst();
  }

  public long create(String prompt, int status, int sortNo) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO ai_agent_quick_question(prompt, status, sort_no, created_at, updated_at)
        VALUES (:prompt, :status, :sortNo, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new MapSqlParameterSource().addValue("prompt", prompt).addValue("status", status).addValue("sortNo", sortNo),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create ai_agent_quick_question");
    }
    return id.longValue();
  }

  public void update(long id, String prompt, int status, int sortNo) {
    var sql =
        """
        UPDATE ai_agent_quick_question
        SET prompt = :prompt, status = :status, sort_no = :sortNo, updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id, "prompt", prompt, "status", status, "sortNo", sortNo));
  }

  public void softDelete(long id) {
    var sql =
        """
        UPDATE ai_agent_quick_question
        SET status = 0, deleted_at = NOW(3), updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id));
  }

  private static void appendWhere(StringBuilder sql, MapSqlParameterSource params, String keyword, Integer status) {
    if (status != null) {
      sql.append("\n  AND status = :status");
      params.addValue("status", status);
    }
    if (keyword != null) {
      sql.append("\n  AND prompt LIKE :keyword");
      params.addValue("keyword", "%" + keyword + "%");
    }
  }

  public record Row(long id, String prompt, int status, int sortNo, java.time.Instant createdAt, java.time.Instant updatedAt) {}
}

