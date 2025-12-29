package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminUserRoleRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminUserRoleRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<String> listRoleCodes(long adminUserId) {
    var sql =
        """
        SELECT r.code
        FROM admin_user_role ur
        JOIN admin_role r ON r.id = ur.role_id
        WHERE ur.admin_user_id = :adminUserId
        ORDER BY r.code ASC
        """;
    return jdbc.queryForList(sql, Map.of("adminUserId", adminUserId), String.class);
  }

  public void replaceUserRoles(long adminUserId, List<Long> roleIds) {
    jdbc.update(
        "DELETE FROM admin_user_role WHERE admin_user_id = :adminUserId",
        Map.of("adminUserId", adminUserId));
    if (roleIds == null || roleIds.isEmpty()) {
      return;
    }
    var sql =
        """
        INSERT INTO admin_user_role(admin_user_id, role_id, created_at)
        VALUES (:adminUserId, :roleId, NOW(3))
        """;
    for (var roleId : roleIds) {
      jdbc.update(sql, Map.of("adminUserId", adminUserId, "roleId", roleId));
    }
  }
}

