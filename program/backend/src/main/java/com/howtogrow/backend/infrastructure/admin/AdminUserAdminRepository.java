package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserAdminRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminUserAdminRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<AdminUserRow> listAll() {
    var sql =
        """
        SELECT id, username, status, created_at
        FROM admin_user
        WHERE deleted_at IS NULL
        ORDER BY id ASC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new AdminUserRow(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getInt("status"),
                rs.getTimestamp("created_at").toInstant()));
  }

  public Optional<AdminUserRow> findByUsername(String username) {
    var sql =
        """
        SELECT id, username, status, created_at
        FROM admin_user
        WHERE username = :username AND deleted_at IS NULL
        LIMIT 1
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("username", username),
            (rs, rowNum) ->
                new AdminUserRow(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getInt("status"),
                    rs.getTimestamp("created_at").toInstant()));
    return rows.stream().findFirst();
  }

  public long create(String username, String passwordHash) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO admin_user(username, password_hash, status, created_at, updated_at)
        VALUES (:username, :passwordHash, 1, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("username", username)
            .addValue("passwordHash", passwordHash),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create admin_user");
    }
    return id.longValue();
  }

  public void updatePassword(long adminUserId, String passwordHash) {
    jdbc.update(
        "UPDATE admin_user SET password_hash = :hash, updated_at = NOW(3) WHERE id = :id AND deleted_at IS NULL",
        Map.of("id", adminUserId, "hash", passwordHash));
  }

  public void setStatus(long adminUserId, int status) {
    jdbc.update(
        "UPDATE admin_user SET status = :status, updated_at = NOW(3) WHERE id = :id AND deleted_at IS NULL",
        Map.of("id", adminUserId, "status", status));
  }

  public record AdminUserRow(long id, String username, int status, java.time.Instant createdAt) {}
}

