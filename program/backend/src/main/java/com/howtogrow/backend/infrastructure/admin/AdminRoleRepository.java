package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRoleRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminRoleRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<RoleRow> listAll() {
    var sql =
        """
        SELECT id, code, name
        FROM admin_role
        ORDER BY id ASC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) -> new RoleRow(rs.getLong("id"), rs.getString("code"), rs.getString("name")));
  }

  public Optional<RoleRow> findByCode(String code) {
    var sql = "SELECT id, code, name FROM admin_role WHERE code = :code LIMIT 1";
    var rows =
        jdbc.query(
            sql,
            Map.of("code", code),
            (rs, rowNum) -> new RoleRow(rs.getLong("id"), rs.getString("code"), rs.getString("name")));
    return rows.stream().findFirst();
  }

  public long create(String code, String name) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO admin_role(code, name, created_at, updated_at)
        VALUES (:code, :name, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("code", code)
            .addValue("name", name),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create admin_role");
    }
    return id.longValue();
  }

  public void update(long roleId, String name) {
    jdbc.update(
        "UPDATE admin_role SET name = :name, updated_at = NOW(3) WHERE id = :id",
        Map.of("id", roleId, "name", name));
  }

  public record RoleRow(long id, String code, String name) {}
}

