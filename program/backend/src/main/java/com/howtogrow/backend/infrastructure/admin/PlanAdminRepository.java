package com.howtogrow.backend.infrastructure.admin;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class PlanAdminRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public PlanAdminRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<PlanRow> listAll() {
    var sql =
        """
        SELECT id, name, days, original_price_cent, price_cent, status
        FROM subscription_plan
        WHERE deleted_at IS NULL
        ORDER BY created_at DESC, id DESC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new PlanRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getInt("days"),
                rs.getInt("original_price_cent"),
                rs.getInt("price_cent"),
                rs.getInt("status")));
  }

  public long create(String name, int days, int originalPriceCent, int priceCent, int status) {
    int sortNo = nextSortNo();
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO subscription_plan(name, days, original_price_cent, price_cent, status, sort_no, created_at, updated_at)
        VALUES (:name, :days, :originalPriceCent, :priceCent, :status, :sortNo, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("name", name)
            .addValue("days", days)
            .addValue("originalPriceCent", originalPriceCent)
            .addValue("priceCent", priceCent)
            .addValue("status", status)
            .addValue("sortNo", sortNo),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create subscription_plan");
    }
    return id.longValue();
  }

  public void update(long id, String name, int days, int originalPriceCent, int priceCent, int status) {
    var sql =
        """
        UPDATE subscription_plan
        SET name = :name, days = :days, original_price_cent = :originalPriceCent, price_cent = :priceCent, status = :status, updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(
        sql,
        Map.of(
            "id", id,
            "name", name,
            "days", days,
            "originalPriceCent", originalPriceCent,
            "priceCent", priceCent,
            "status", status));
  }

  public void softDelete(long id) {
    var sql =
        """
        UPDATE subscription_plan
        SET status = 0, deleted_at = NOW(3), updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id));
  }

  private int nextSortNo() {
    var sql =
        """
        SELECT COALESCE(MAX(sort_no), 0) + 1 AS next_sort_no
        FROM subscription_plan
        WHERE deleted_at IS NULL
        """;
    Integer next = jdbc.queryForObject(sql, Map.of(), Integer.class);
    return next == null ? 1 : next;
  }

  public record PlanRow(long id, String name, int days, int originalPriceCent, int priceCent, int status) {}
}
