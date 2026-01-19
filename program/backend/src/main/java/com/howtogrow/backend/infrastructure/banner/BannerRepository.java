package com.howtogrow.backend.infrastructure.banner;

import com.howtogrow.backend.infrastructure.db.SqlPagination;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class BannerRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public BannerRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long countAdmin(String keyword, Integer status) {
    var params = new MapSqlParameterSource();
    var sql = new StringBuilder("SELECT COUNT(*) FROM banner WHERE deleted_at IS NULL");
    appendWhere(sql, params, keyword, status);
    Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
    return count == null ? 0 : count;
  }

  public long countActive() {
    var sql = "SELECT COUNT(*) FROM banner WHERE status = 1 AND deleted_at IS NULL";
    Long count = jdbc.queryForObject(sql, Map.of(), Long.class);
    return count == null ? 0 : count;
  }

  public List<Long> listActiveIdsForUpdate() {
    var sql =
        """
        SELECT id
        FROM banner
        WHERE status = 1 AND deleted_at IS NULL
        FOR UPDATE
        """;
    return jdbc.queryForList(sql, Map.of(), Long.class);
  }

  public List<BannerRow> listAdmin() {
    var sql =
        """
        SELECT id, title, image_url, html_content, status, sort_no, created_at, updated_at
        FROM banner
        WHERE deleted_at IS NULL
        ORDER BY sort_no ASC, id DESC
        """;
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new BannerRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("image_url"),
                rs.getString("html_content"),
                rs.getInt("status"),
                rs.getInt("sort_no"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()));
  }

  public List<BannerRow> listAdminPage(int offset, int limit, String keyword, Integer status) {
    var params = new MapSqlParameterSource().addValue("offset", offset).addValue("limit", limit);
    var sql =
        new StringBuilder(
            """
            SELECT id, title, image_url, html_content, status, sort_no, created_at, updated_at
            FROM banner
            WHERE deleted_at IS NULL
            """);
    appendWhere(sql, params, keyword, status);
    sql.append("\nORDER BY sort_no ASC, id DESC\n").append(SqlPagination.limitOffset(offset, limit));
    return jdbc.query(
        sql.toString(),
        params,
        (rs, rowNum) ->
            new BannerRow(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("image_url"),
                rs.getString("html_content"),
                rs.getInt("status"),
                rs.getInt("sort_no"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()));
  }

  private static void appendWhere(StringBuilder sql, MapSqlParameterSource params, String keyword, Integer status) {
    if (status != null) {
      sql.append("\n  AND status = :status");
      params.addValue("status", status);
    }
    if (keyword != null) {
      sql.append("\n  AND title LIKE :keyword");
      params.addValue("keyword", "%" + keyword + "%");
    }
  }

  public Optional<BannerRow> findById(long id) {
    var sql =
        """
        SELECT id, title, image_url, html_content, status, sort_no, created_at, updated_at
        FROM banner
        WHERE id = :id AND deleted_at IS NULL
        """;
    var rows = jdbc.query(sql, Map.of("id", id), (rs, rowNum) ->
        new BannerRow(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("image_url"),
            rs.getString("html_content"),
            rs.getInt("status"),
            rs.getInt("sort_no"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("updated_at").toInstant()));
    return rows.stream().findFirst();
  }

  public long create(String title, String imageUrl, String htmlContent, int status, int sortNo) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO banner(title, image_url, html_content, status, sort_no, created_at, updated_at)
        VALUES (:title, :imageUrl, :htmlContent, :status, :sortNo, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("title", title)
            .addValue("imageUrl", imageUrl)
            .addValue("htmlContent", htmlContent)
            .addValue("status", status)
            .addValue("sortNo", sortNo),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create banner");
    }
    return id.longValue();
  }

  public void update(long id, String title, String imageUrl, String htmlContent, int status, int sortNo) {
    var sql =
        """
        UPDATE banner
        SET title = :title,
            image_url = :imageUrl,
            html_content = :htmlContent,
            status = :status,
            sort_no = :sortNo,
            updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(
        sql,
        Map.of(
            "id", id,
            "title", title,
            "imageUrl", imageUrl,
            "htmlContent", htmlContent,
            "status", status,
            "sortNo", sortNo));
  }

  public void softDelete(long id) {
    var sql =
        """
        UPDATE banner
        SET status = 0, deleted_at = NOW(3), updated_at = NOW(3)
        WHERE id = :id AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("id", id));
  }

  public List<BannerListRow> listActiveForMiniprogram(int limit) {
    var sql =
        """
        SELECT id, title, image_url, sort_no
        FROM banner
        WHERE status = 1 AND deleted_at IS NULL
        ORDER BY sort_no ASC, id DESC
        """
            + SqlPagination.limit(limit);
    return jdbc.query(
        sql,
        Map.of(),
        (rs, rowNum) ->
            new BannerListRow(
                rs.getLong("id"), rs.getString("title"), rs.getString("image_url"), rs.getInt("sort_no")));
  }

  public Optional<String> findActiveHtmlContent(long id) {
    var sql =
        """
        SELECT html_content
        FROM banner
        WHERE id = :id AND status = 1 AND deleted_at IS NULL
        """;
    var rows = jdbc.queryForList(sql, Map.of("id", id), String.class);
    return rows.stream().findFirst();
  }

  public record BannerRow(
      long id,
      String title,
      String imageUrl,
      String htmlContent,
      int status,
      int sortNo,
      java.time.Instant createdAt,
      java.time.Instant updatedAt) {}

  public record BannerListRow(long id, String title, String imageUrl, int sortNo) {}
}
