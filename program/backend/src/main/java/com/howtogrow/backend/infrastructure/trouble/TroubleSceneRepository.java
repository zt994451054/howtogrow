package com.howtogrow.backend.infrastructure.trouble;

import com.howtogrow.backend.infrastructure.db.SqlPagination;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class TroubleSceneRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public TroubleSceneRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countAdmin(String keyword, Integer ageYear) {
    var params = new MapSqlParameterSource();
    var sql = new StringBuilder("SELECT COUNT(*) FROM trouble_scene WHERE deleted_at IS NULL");
    appendWhere(sql, params, keyword, ageYear);
    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count == null ? 0L : count;
  }

  public List<TroubleSceneRow> listActive() {
    var sql =
        """
        SELECT id, name, logo_url, min_age, max_age
        FROM trouble_scene
        WHERE status = 1 AND deleted_at IS NULL
        ORDER BY id DESC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new TroubleSceneRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("logo_url"),
                rs.getInt("min_age"),
                rs.getInt("max_age")));
  }

  public List<TroubleSceneAdminRow> listAdmin() {
    var sql =
        """
        SELECT id, name, logo_url, min_age, max_age, status, created_at, updated_at
        FROM trouble_scene
        WHERE deleted_at IS NULL
        ORDER BY id DESC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new TroubleSceneAdminRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("logo_url"),
                rs.getInt("min_age"),
                rs.getInt("max_age"),
                rs.getInt("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()));
  }

  public List<TroubleSceneAdminRow> listAdminPage(int offset, int limit, String keyword, Integer ageYear) {
    var params = new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    var sql =
        new StringBuilder(
            """
            SELECT id, name, logo_url, min_age, max_age, status, created_at, updated_at
            FROM trouble_scene
            WHERE deleted_at IS NULL
            """);
    appendWhere(sql, params, keyword, ageYear);
    sql.append("\nORDER BY id DESC\n").append(SqlPagination.limitOffset(offset, limit));
    return jdbc.query(
        sql.toString(),
        params,
        (rs, rowNum) ->
            new TroubleSceneAdminRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("logo_url"),
                rs.getInt("min_age"),
                rs.getInt("max_age"),
                rs.getInt("status"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()));
  }

  private static void appendWhere(StringBuilder sql, MapSqlParameterSource params, String keyword, Integer ageYear) {
    if (keyword != null) {
      sql.append("\n  AND name LIKE :keyword");
      params.addValue("keyword", "%" + keyword + "%");
    }
    if (ageYear != null) {
      sql.append("\n  AND min_age <= :ageYear AND max_age >= :ageYear");
      params.addValue("ageYear", ageYear);
    }
  }

  public Optional<TroubleSceneAdminRow> findById(long id) {
    var sql =
        """
        SELECT id, name, logo_url, min_age, max_age, status, created_at, updated_at
        FROM trouble_scene
        WHERE id = :id AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("id", id),
            (rs, rowNum) ->
                new TroubleSceneAdminRow(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("logo_url"),
                    rs.getInt("min_age"),
                    rs.getInt("max_age"),
                    rs.getInt("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("updated_at").toInstant()));
    return rows.stream().findFirst();
  }

  public long create(String name, String logoUrl, int minAge, int maxAge) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO trouble_scene(name, logo_url, min_age, max_age, status, created_at, updated_at)
        VALUES (:name, :logoUrl, :minAge, :maxAge, 1, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("name", name)
            .addValue("logoUrl", logoUrl)
            .addValue("minAge", minAge)
            .addValue("maxAge", maxAge),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create trouble_scene");
    }
    return id.longValue();
  }

  public void update(long id, String name, String logoUrl, int minAge, int maxAge) {
    var sql =
        """
        UPDATE trouble_scene
        SET name = :name, logo_url = :logoUrl, min_age = :minAge, max_age = :maxAge, updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id, "name", name, "logoUrl", logoUrl, "minAge", minAge, "maxAge", maxAge));
  }

  public void softDelete(long id) {
    var sql =
        """
        UPDATE trouble_scene
        SET status = 0,
            deleted_at = NOW(3),
            updated_at = NOW(3),
            name = CONCAT(name, '#', id)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id));
  }

  public Map<String, Long> mapActiveIdsByNames(List<String> names) {
    if (names == null || names.isEmpty()) {
      return Map.of();
    }
    var sql =
        """
        SELECT id, name
        FROM trouble_scene
        WHERE status = 1 AND deleted_at IS NULL
          AND name IN (:names)
        """;
    var rows = jdbc.query(sql, Map.of("names", names), (rs, rowNum) -> Map.entry(rs.getString("name"), rs.getLong("id")));
    var out = new HashMap<String, Long>();
    for (var e : rows) {
      out.put(e.getKey(), e.getValue());
    }
    return out;
  }

  public List<Long> listActiveIds(List<Long> ids) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    var sql =
        """
        SELECT id
        FROM trouble_scene
        WHERE status = 1 AND deleted_at IS NULL
          AND id IN (:ids)
        """
            + SqlPagination.limit(ids.size());
    return jdbc.queryForList(sql, Map.of("ids", ids), Long.class);
  }

  public record TroubleSceneRow(long id, String name, String logoUrl, int minAge, int maxAge) {}

  public record TroubleSceneAdminRow(
      long id,
      String name,
      String logoUrl,
      int minAge,
      int maxAge,
      int status,
      java.time.Instant createdAt,
      java.time.Instant updatedAt) {}
}
