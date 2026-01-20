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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AiChatMessageRepository {
  private static final RowMapper<AiChatMessageRow> ROW_MAPPER = (rs, rowNum) -> toRow(rs);
  private final NamedParameterJdbcTemplate jdbc;

  public AiChatMessageRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insert(long sessionId, long userId, String role, String content) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO ai_chat_message(session_id, user_id, role, content, created_at)
        VALUES (:sessionId, :userId, :role, :content, NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("sessionId", sessionId)
            .addValue("userId", userId)
            .addValue("role", role)
            .addValue("content", content),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create ai_chat_message");
    }
    return id.longValue();
  }

  public List<AiChatMessageRow> listRecent(long sessionId, int limit) {
    var sql =
        """
        SELECT id, session_id, user_id, role, content, created_at
        FROM ai_chat_message
        WHERE session_id = :sessionId
        ORDER BY id DESC
        """;
    sql = sql + SqlPagination.limit(limit);
    var rows = jdbc.query(sql, Map.of("sessionId", sessionId), ROW_MAPPER);
    java.util.Collections.reverse(rows);
    return rows;
  }

  public List<AiChatMessageRow> listPageDesc(long sessionId, int limit, Long beforeMessageId) {
    var sql =
        """
        SELECT id, session_id, user_id, role, content, created_at
        FROM ai_chat_message
        WHERE session_id = :sessionId
          AND (:beforeId IS NULL OR id < :beforeId)
        ORDER BY id DESC
        """;
    sql = sql + SqlPagination.limit(limit);
    return jdbc.query(
        sql,
        new MapSqlParameterSource()
            .addValue("sessionId", sessionId)
            .addValue("beforeId", beforeMessageId),
        ROW_MAPPER);
  }

  public Optional<AiChatMessageRow> findById(long messageId) {
    var sql =
        """
        SELECT id, session_id, user_id, role, content, created_at
        FROM ai_chat_message
        WHERE id = :id
        """;
    var rows = jdbc.query(sql, Map.of("id", messageId), ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public boolean existsAssistantMessageByUser(long userId) {
    var sql =
        """
        SELECT 1
        FROM ai_chat_message
        WHERE user_id = :userId
          AND role = 'assistant'
        LIMIT 1
        """;
    var rows = jdbc.queryForList(sql, Map.of("userId", userId));
    return rows != null && !rows.isEmpty();
  }

  private static AiChatMessageRow toRow(ResultSet rs) throws SQLException {
    Instant createdAt = null;
    var ts = rs.getTimestamp("created_at");
    if (ts != null) {
      createdAt = ts.toInstant();
    }
    return new AiChatMessageRow(
        rs.getLong("id"),
        rs.getLong("session_id"),
        rs.getLong("user_id"),
        rs.getString("role"),
        rs.getString("content"),
        createdAt);
  }

  public record AiChatMessageRow(
      long id, long sessionId, long userId, String role, String content, Instant createdAt) {}
}
