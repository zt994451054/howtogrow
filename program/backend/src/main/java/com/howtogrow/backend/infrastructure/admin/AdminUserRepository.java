package com.howtogrow.backend.infrastructure.admin;

import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminUserRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<AdminUserRow> findByUsername(String username) {
    var sql =
        """
        SELECT id, username, password_hash, status
        FROM admin_user
        WHERE username = :username AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("username", username),
            (rs, rowNum) ->
                new AdminUserRow(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getInt("status")));
    return rows.stream().findFirst();
  }

  public Optional<AdminUserRow> findById(long adminUserId) {
    var sql =
        """
        SELECT id, username, password_hash, status
        FROM admin_user
        WHERE id = :id AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("id", adminUserId),
            (rs, rowNum) ->
                new AdminUserRow(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getInt("status")));
    return rows.stream().findFirst();
  }

  public record AdminUserRow(long id, String username, String passwordHash, int status) {}
}

