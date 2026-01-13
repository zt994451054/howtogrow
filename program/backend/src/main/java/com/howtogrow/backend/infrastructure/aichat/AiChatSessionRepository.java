package com.howtogrow.backend.infrastructure.aichat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AiChatSessionRepository {
  private static final RowMapper<AiChatSessionRow> ROW_MAPPER = (rs, rowNum) -> toRow(rs);
  private final NamedParameterJdbcTemplate jdbc;

  public AiChatSessionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long create(long userId, Long childId) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO ai_chat_session(user_id, child_id, status, last_active_at, created_at, updated_at)
        VALUES (:userId, :childId, 'ACTIVE', NOW(3), NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("childId", childId),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create ai_chat_session");
    }
    return id.longValue();
  }

  public void setTitleIfBlank(long sessionId, String title) {
    if (title == null || title.isBlank()) {
      return;
    }
    jdbc.update(
        """
        UPDATE ai_chat_session
        SET title = :title, updated_at = NOW(3)
        WHERE id = :id AND (title IS NULL OR title = '')
        """,
        Map.of("id", sessionId, "title", title));
  }

  public Optional<AiChatSessionRow> findById(long sessionId) {
    var sql =
        """
        SELECT id, user_id, child_id, title, status, last_active_at
        FROM ai_chat_session
        WHERE id = :id
        """;
    var rows = jdbc.query(sql, Map.of("id", sessionId), ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public List<AiChatSessionRow> listByUser(long userId, int limit) {
    var sql =
        """
        SELECT id, user_id, child_id, title, status, last_active_at
        FROM ai_chat_session
        WHERE user_id = :userId
        ORDER BY last_active_at DESC, id DESC
        """;
    sql = sql + SqlPagination.limit(limit);
    return jdbc.query(sql, Map.of("userId", userId), ROW_MAPPER);
  }

  public void touch(long sessionId) {
    jdbc.update(
        "UPDATE ai_chat_session SET last_active_at = NOW(3), updated_at = NOW(3) WHERE id = :id",
        Map.of("id", sessionId));
  }

  private static AiChatSessionRow toRow(ResultSet rs) throws SQLException {
    Instant lastActiveAt = null;
    var ts = rs.getTimestamp("last_active_at");
    if (ts != null) {
      lastActiveAt = ts.toInstant();
    }
    Long childId = null;
    var rawChildId = rs.getObject("child_id");
    if (rawChildId instanceof Number n) {
      childId = n.longValue();
    }
    return new AiChatSessionRow(
        rs.getLong("id"),
        rs.getLong("user_id"),
        childId,
        rs.getString("title"),
        rs.getString("status"),
        lastActiveAt);
  }

  public record AiChatSessionRow(
      long id, long userId, Long childId, String title, String status, Instant lastActiveAt) {}
}
