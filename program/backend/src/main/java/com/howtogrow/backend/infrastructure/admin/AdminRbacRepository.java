package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AdminRbacRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AdminRbacRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<String> listPermissionCodes(long adminUserId) {
    var sql =
        """
        SELECT DISTINCT p.code
        FROM admin_permission p
        JOIN admin_role_permission rp ON rp.permission_id = p.id
        JOIN admin_user_role ur ON ur.role_id = rp.role_id
        WHERE ur.admin_user_id = :adminUserId
        ORDER BY p.code ASC
        """;
    return jdbc.queryForList(sql, Map.of("adminUserId", adminUserId), String.class);
  }
}

