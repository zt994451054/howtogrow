package com.howtogrow.backend.infrastructure.parenting;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DailyParentingDiaryRepository {
  private static final RowMapper<DiaryRow> ROW_MAPPER = (rs, rowNum) -> toRow(rs);
  private final NamedParameterJdbcTemplate jdbc;

  public DailyParentingDiaryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<DiaryRow> listByUserChildBetween(long userId, long childId, LocalDate fromInclusive, LocalDate toInclusive) {
    var sql =
        """
        SELECT record_date, content, image_url
        FROM daily_parenting_diary
        WHERE user_id = :userId
          AND child_id = :childId
          AND record_date >= :fromInclusive
          AND record_date <= :toInclusive
        ORDER BY record_date ASC
        """;
    return jdbc.query(
        sql,
        Map.of("userId", userId, "childId", childId, "fromInclusive", fromInclusive, "toInclusive", toInclusive),
        ROW_MAPPER);
  }

  public Optional<DiaryRow> findByUserChildDate(long userId, long childId, LocalDate recordDate) {
    var sql =
        """
        SELECT record_date, content, image_url
        FROM daily_parenting_diary
        WHERE user_id = :userId
          AND child_id = :childId
          AND record_date = :recordDate
        LIMIT 1
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("userId", userId, "childId", childId, "recordDate", recordDate),
            ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public void upsert(long userId, long childId, LocalDate recordDate, String content, String imageUrl) {
    var existing = findId(userId, childId, recordDate);
    if (existing.isPresent()) {
      jdbc.update(
          """
          UPDATE daily_parenting_diary
          SET content = :content, image_url = :imageUrl, updated_at = NOW(3)
          WHERE id = :id
          """,
          new MapSqlParameterSource()
              .addValue("id", existing.get())
              .addValue("content", content)
              .addValue("imageUrl", imageUrl));
      return;
    }

    KeyHolder kh = new GeneratedKeyHolder();
    jdbc.update(
        """
        INSERT INTO daily_parenting_diary(user_id, child_id, record_date, content, image_url, created_at, updated_at)
        VALUES (:userId, :childId, :recordDate, :content, :imageUrl, NOW(3), NOW(3))
        """,
        new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("childId", childId)
            .addValue("recordDate", recordDate)
            .addValue("content", content)
            .addValue("imageUrl", imageUrl),
        kh);
    if (kh.getKey() == null) {
      throw new IllegalStateException("failed to insert daily_parenting_diary");
    }
  }

  private static DiaryRow toRow(ResultSet rs) throws SQLException {
    return new DiaryRow(
        rs.getDate("record_date").toLocalDate(),
        rs.getString("content"),
        rs.getString("image_url"));
  }

  private Optional<Long> findId(long userId, long childId, LocalDate recordDate) {
    var sql =
        """
        SELECT id
        FROM daily_parenting_diary
        WHERE user_id = :userId AND child_id = :childId AND record_date = :recordDate
        LIMIT 1
        """;
    var rows =
        jdbc.queryForList(
            sql, Map.of("userId", userId, "childId", childId, "recordDate", recordDate), Long.class);
    return rows.stream().findFirst();
  }

  public record DiaryRow(LocalDate recordDate, String content, String imageUrl) {}
}
