package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRolePermissionRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminRolePermissionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<String> listPermissionCodes(long roleId) {
    var sql =
        """
        SELECT p.code
        FROM admin_role_permission rp
        JOIN admin_permission p ON p.id = rp.permission_id
        WHERE rp.role_id = :roleId
        ORDER BY p.code ASC
        """;
    return jdbc.queryForList(sql, Map.of("roleId", roleId), String.class);
  }

  public void replaceRolePermissions(long roleId, List<Long> permissionIds) {
    jdbc.update("DELETE FROM admin_role_permission WHERE role_id = :roleId", Map.of("roleId", roleId));
    if (permissionIds == null || permissionIds.isEmpty()) {
      return;
    }
    var sql =
        """
        INSERT INTO admin_role_permission(role_id, permission_id, created_at)
        VALUES (:roleId, :permissionId, NOW(3))
        """;
    for (var pid : permissionIds) {
      jdbc.update(sql, Map.of("roleId", roleId, "permissionId", pid));
    }
  }
}

