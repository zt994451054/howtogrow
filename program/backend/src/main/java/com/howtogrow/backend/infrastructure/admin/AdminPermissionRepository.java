package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminPermissionRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminPermissionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<PermissionRow> listAll() {
    var sql =
        """
        SELECT id, code, name
        FROM admin_permission
        ORDER BY id ASC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new PermissionRow(rs.getLong("id"), rs.getString("code"), rs.getString("name")));
  }

  public List<Long> listPermissionIdsByCodes(List<String> codes) {
    if (codes == null || codes.isEmpty()) {
      return List.of();
    }
    var sql = "SELECT id FROM admin_permission WHERE code IN (:codes)";
    return jdbc.queryForList(sql, Map.of("codes", codes), Long.class);
  }

  public record PermissionRow(long id, String code, String name) {}
}

