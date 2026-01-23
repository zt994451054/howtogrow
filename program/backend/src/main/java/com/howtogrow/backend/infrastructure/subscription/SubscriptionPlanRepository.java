package com.howtogrow.backend.infrastructure.subscription;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubscriptionPlanRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public SubscriptionPlanRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<PlanRow> listActivePlans() {
    var sql =
        """
        SELECT id, name, days, original_price_cent, price_cent
        FROM subscription_plan
        WHERE status = 1 AND deleted_at IS NULL
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
                rs.getInt("price_cent")));
  }

  public PlanRow requirePlan(long planId) {
    var sql =
        """
        SELECT id, name, days, original_price_cent, price_cent
        FROM subscription_plan
        WHERE id = :id AND status = 1 AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("id", planId),
            (rs, rowNum) ->
                new PlanRow(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("days"),
                    rs.getInt("original_price_cent"),
                    rs.getInt("price_cent")));
    return rows.stream().findFirst().orElseThrow();
  }

  public java.util.Optional<PlanRow> findById(long planId) {
    var sql =
        """
        SELECT id, name, days, original_price_cent, price_cent
        FROM subscription_plan
        WHERE id = :id AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("id", planId),
            (rs, rowNum) ->
                new PlanRow(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("days"),
                    rs.getInt("original_price_cent"),
                    rs.getInt("price_cent")));
    return rows.stream().findFirst();
  }

  public java.util.Optional<PlanRow> findActiveById(long planId) {
    var sql =
        """
        SELECT id, name, days, original_price_cent, price_cent
        FROM subscription_plan
        WHERE id = :id AND status = 1 AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("id", planId),
            (rs, rowNum) ->
                new PlanRow(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getInt("days"),
                    rs.getInt("original_price_cent"),
                    rs.getInt("price_cent")));
    return rows.stream().findFirst();
  }

  public record PlanRow(long planId, String name, int days, int originalPriceCent, int priceCent) {}
}
