package com.howtogrow.backend.infrastructure.trouble;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class DailyTroubleRecordRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public DailyTroubleRecordRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long upsertRecord(long userId, long childId, LocalDate recordDate) {
    var existing = findRecordId(userId, childId, recordDate);
    if (existing.isPresent()) {
      jdbc.update(
          "UPDATE daily_trouble_record SET updated_at = NOW(3) WHERE id = :id",
          Map.of("id", existing.get()));
      return existing.get();
    }

    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO daily_trouble_record(user_id, child_id, record_date, created_at, updated_at)
        VALUES (:userId, :childId, :recordDate, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("childId", childId)
            .addValue("recordDate", recordDate),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to upsert daily_trouble_record");
    }
    return id.longValue();
  }

  public void replaceScenes(long recordId, List<Long> sceneIds) {
    jdbc.update("DELETE FROM daily_trouble_record_scene WHERE record_id = :rid", Map.of("rid", recordId));
    if (sceneIds == null || sceneIds.isEmpty()) {
      return;
    }
    var unique = new ArrayList<Long>();
    var seen = new HashSet<Long>();
    for (var id : sceneIds) {
      if (id != null && id > 0 && seen.add(id)) {
        unique.add(id);
      }
    }
    for (var sceneId : unique) {
      jdbc.update(
          """
          INSERT INTO daily_trouble_record_scene(record_id, scene_id, created_at)
          VALUES (:rid, :sid, NOW(3))
          """,
          Map.of("rid", recordId, "sid", sceneId));
    }
  }

  public Optional<Long> findRecordId(long userId, long childId, LocalDate recordDate) {
    var sql =
        """
        SELECT id
        FROM daily_trouble_record
        WHERE user_id = :userId AND child_id = :childId AND record_date = :recordDate
        LIMIT 1
        """;
    var rows =
        jdbc.queryForList(
            sql, Map.of("userId", userId, "childId", childId, "recordDate", recordDate), Long.class);
    return rows.stream().findFirst();
  }

  public List<Long> listActiveSceneIds(long userId, long childId, LocalDate recordDate) {
    var sql =
        """
        SELECT s.scene_id
        FROM daily_trouble_record r
        JOIN daily_trouble_record_scene s ON s.record_id = r.id
        JOIN trouble_scene ts ON ts.id = s.scene_id AND ts.status = 1 AND ts.deleted_at IS NULL
        WHERE r.user_id = :userId AND r.child_id = :childId AND r.record_date = :recordDate
        ORDER BY s.id ASC
        """;
    return jdbc.queryForList(
        sql, Map.of("userId", userId, "childId", childId, "recordDate", recordDate), Long.class);
  }

  public List<TroubleSceneRepository.TroubleSceneRow> listActiveScenes(long userId, long childId, LocalDate recordDate) {
    var sql =
        """
        SELECT ts.id, ts.name, ts.logo_url, ts.min_age, ts.max_age
        FROM daily_trouble_record r
        JOIN daily_trouble_record_scene s ON s.record_id = r.id
        JOIN trouble_scene ts ON ts.id = s.scene_id AND ts.status = 1 AND ts.deleted_at IS NULL
        WHERE r.user_id = :userId AND r.child_id = :childId AND r.record_date = :recordDate
        ORDER BY s.id ASC
        """;
    return jdbc.query(
        sql,
        Map.of("userId", userId, "childId", childId, "recordDate", recordDate),
        (rs, rowNum) ->
            new TroubleSceneRepository.TroubleSceneRow(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("logo_url"),
                rs.getInt("min_age"),
                rs.getInt("max_age")));
  }

  public Map<LocalDate, List<TroubleSceneRepository.TroubleSceneRow>> listActiveScenesByDayBetween(
      long userId, long childId, LocalDate fromInclusive, LocalDate toInclusive) {
    var sql =
        """
        SELECT r.record_date, ts.id, ts.name, ts.logo_url, ts.min_age, ts.max_age
        FROM daily_trouble_record r
        JOIN daily_trouble_record_scene s ON s.record_id = r.id
        JOIN trouble_scene ts ON ts.id = s.scene_id AND ts.status = 1 AND ts.deleted_at IS NULL
        WHERE r.user_id = :userId
          AND r.child_id = :childId
          AND r.record_date >= :fromInclusive
          AND r.record_date <= :toInclusive
        ORDER BY r.record_date ASC, s.id ASC
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("userId", userId, "childId", childId, "fromInclusive", fromInclusive, "toInclusive", toInclusive),
            (rs, rowNum) ->
                Map.entry(
                    rs.getDate("record_date").toLocalDate(),
                    new TroubleSceneRepository.TroubleSceneRow(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("logo_url"),
                        rs.getInt("min_age"),
                        rs.getInt("max_age"))));
    var map = new HashMap<LocalDate, List<TroubleSceneRepository.TroubleSceneRow>>();
    for (var e : rows) {
      map.computeIfAbsent(e.getKey(), (k) -> new ArrayList<>()).add(e.getValue());
    }
    return map;
  }
}
