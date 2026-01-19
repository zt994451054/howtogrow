package com.howtogrow.backend.infrastructure.question;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.howtogrow.backend.infrastructure.db.SqlPagination;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuestionRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuestionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Long> pickRandomQuestionIds(int ageYear, int limit) {
    var sql =
        """
        SELECT id
        FROM question
        WHERE min_age <= :ageYear AND max_age >= :ageYear
          AND status = 1
          AND deleted_at IS NULL
        ORDER BY RAND()
        """;
    sql = sql + SqlPagination.limit(limit);
    return jdbc.queryForList(sql, Map.of("ageYear", ageYear), Long.class);
  }

  public Optional<Long> pickRandomQuestionIdExcluding(int ageYear, List<Long> excludedQuestionIds) {
    if (excludedQuestionIds == null || excludedQuestionIds.isEmpty()) {
      return pickRandomQuestionIds(ageYear, 1).stream().findFirst();
    }
    var sql =
        """
        SELECT id
        FROM question
        WHERE min_age <= :ageYear AND max_age >= :ageYear
          AND status = 1
          AND deleted_at IS NULL
          AND id NOT IN (:excludedIds)
        ORDER BY RAND()
        LIMIT 1
        """;
    var rows =
        jdbc.queryForList(
            sql, Map.of("ageYear", ageYear, "excludedIds", excludedQuestionIds), Long.class);
    return rows.stream().findFirst();
  }

  public List<Long> pickRandomQuestionIdsExcluding(int ageYear, List<Long> excludedQuestionIds, int limit) {
    if (limit <= 0) {
      return List.of();
    }
    if (excludedQuestionIds == null || excludedQuestionIds.isEmpty()) {
      return pickRandomQuestionIds(ageYear, limit);
    }
    var sql =
        """
        SELECT id
        FROM question
        WHERE min_age <= :ageYear AND max_age >= :ageYear
          AND status = 1
          AND deleted_at IS NULL
          AND id NOT IN (:excludedIds)
        ORDER BY RAND()
        """
            + SqlPagination.limit(limit);
    return jdbc.queryForList(
        sql, Map.of("ageYear", ageYear, "excludedIds", excludedQuestionIds), Long.class);
  }

  public List<Long> pickRandomQuestionIdsByTroubleScenes(
      int ageYear, List<Long> troubleSceneIds, List<Long> excludedQuestionIds, int limit) {
    if (limit <= 0) {
      return List.of();
    }
    if (troubleSceneIds == null || troubleSceneIds.isEmpty()) {
      return List.of();
    }

    var params = new java.util.HashMap<String, Object>();
    params.put("ageYear", ageYear);
    params.put("sceneIds", troubleSceneIds);
    params.put("limit", limit);

    var sql =
        """
        SELECT DISTINCT q.id
        FROM question q
        JOIN question_trouble_scene qts ON qts.question_id = q.id
        WHERE q.min_age <= :ageYear AND q.max_age >= :ageYear
          AND q.status = 1
          AND q.deleted_at IS NULL
          AND qts.scene_id IN (:sceneIds)
        """;
    if (excludedQuestionIds != null && !excludedQuestionIds.isEmpty()) {
      sql += " AND q.id NOT IN (:excludedIds) ";
      params.put("excludedIds", excludedQuestionIds);
    }
    sql += " ORDER BY RAND() " + SqlPagination.limit(limit);
    return jdbc.queryForList(sql, params, Long.class);
  }

  public Optional<QuestionTypeRow> findQuestionType(long questionId) {
    var sql =
        """
        SELECT id, question_type
        FROM question
        WHERE id = :id AND deleted_at IS NULL
        """;
    var rows =
        jdbc.query(
            sql,
            Map.of("id", questionId),
            (rs, rowNum) -> new QuestionTypeRow(rs.getLong("id"), rs.getString("question_type")));
    return rows.stream().findFirst();
  }

  public record QuestionTypeRow(long id, String questionType) {}
}
