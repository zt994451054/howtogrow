package com.howtogrow.backend.infrastructure.assessment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DailyAssessmentHistoryRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public DailyAssessmentHistoryRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<RecordRow> listByUserId(long userId, int limit, int offset) {
    var sql =
        """
        SELECT
          a.id AS assessment_id,
          a.child_id,
          c.nickname AS child_name,
          a.submitted_at,
          s.content AS ai_summary
        FROM daily_assessment a
        LEFT JOIN child c ON c.id = a.child_id
        LEFT JOIN ai_assessment_summary s
          ON s.assessment_id = a.id
         AND s.user_id = a.user_id
        WHERE a.user_id = :userId
        ORDER BY a.submitted_at DESC, a.id DESC
        LIMIT :limit OFFSET :offset
        """;
    return jdbc.query(sql, Map.of("userId", userId, "limit", limit, "offset", offset), RECORD_MAPPER);
  }

  public Optional<RecordRow> findByUserIdAndAssessmentId(long userId, long assessmentId) {
    var sql =
        """
        SELECT
          a.id AS assessment_id,
          a.child_id,
          c.nickname AS child_name,
          a.submitted_at,
          s.content AS ai_summary
        FROM daily_assessment a
        LEFT JOIN child c ON c.id = a.child_id
        LEFT JOIN ai_assessment_summary s
          ON s.assessment_id = a.id
         AND s.user_id = a.user_id
        WHERE a.user_id = :userId
          AND a.id = :assessmentId
        """;
    var rows = jdbc.query(sql, Map.of("userId", userId, "assessmentId", assessmentId), RECORD_MAPPER);
    return rows.stream().findFirst();
  }

  public List<RecordRow> listByUserIdAndChildIdBetween(
      long userId, long childId, Instant fromInclusive, Instant toExclusive) {
    var sql =
        """
        SELECT
          a.id AS assessment_id,
          a.child_id,
          c.nickname AS child_name,
          a.submitted_at,
          s.content AS ai_summary
        FROM daily_assessment a
        LEFT JOIN child c ON c.id = a.child_id
        LEFT JOIN ai_assessment_summary s
          ON s.assessment_id = a.id
         AND s.user_id = a.user_id
        WHERE a.user_id = :userId
          AND a.child_id = :childId
          AND a.submitted_at >= :fromInclusive
          AND a.submitted_at < :toExclusive
        ORDER BY a.submitted_at ASC, a.id ASC
        """;
    return jdbc.query(
        sql,
        Map.of(
            "userId", userId,
            "childId", childId,
            "fromInclusive", Timestamp.from(fromInclusive),
            "toExclusive", Timestamp.from(toExclusive)),
        RECORD_MAPPER);
  }

  public List<ItemRow> listItems(long assessmentId) {
    var sql =
        """
        SELECT id, assessment_id, question_id, display_order
        FROM daily_assessment_item
        WHERE assessment_id = :assessmentId
        ORDER BY display_order ASC, id ASC
        """;
    return jdbc.query(sql, Map.of("assessmentId", assessmentId), ITEM_MAPPER);
  }

  public List<AnswerRow> listAnswers(long assessmentId) {
    var sql =
        """
        SELECT i.question_id, a.option_id
        FROM daily_assessment_answer a
        JOIN daily_assessment_item i ON i.id = a.assessment_item_id
        WHERE a.assessment_id = :assessmentId
        ORDER BY i.display_order ASC, a.option_id ASC
        """;
    return jdbc.query(sql, Map.of("assessmentId", assessmentId), ANSWER_MAPPER);
  }

  private static final RowMapper<RecordRow> RECORD_MAPPER = (rs, rowNum) -> toRecordRow(rs);
  private static final RowMapper<ItemRow> ITEM_MAPPER = (rs, rowNum) -> toItemRow(rs);
  private static final RowMapper<AnswerRow> ANSWER_MAPPER = (rs, rowNum) -> toAnswerRow(rs);

  private static RecordRow toRecordRow(ResultSet rs) throws SQLException {
    Instant submittedAt = null;
    Timestamp ts = rs.getTimestamp("submitted_at");
    if (ts != null) submittedAt = ts.toInstant();
    return new RecordRow(
        rs.getLong("assessment_id"),
        rs.getLong("child_id"),
        rs.getString("child_name"),
        submittedAt,
        rs.getString("ai_summary"));
  }

  private static ItemRow toItemRow(ResultSet rs) throws SQLException {
    return new ItemRow(
        rs.getLong("id"),
        rs.getLong("assessment_id"),
        rs.getLong("question_id"),
        rs.getInt("display_order"));
  }

  private static AnswerRow toAnswerRow(ResultSet rs) throws SQLException {
    return new AnswerRow(rs.getLong("question_id"), rs.getLong("option_id"));
  }

  public record RecordRow(
      long assessmentId, long childId, String childName, Instant submittedAt, String aiSummary) {}

  public record ItemRow(long itemId, long assessmentId, long questionId, int displayOrder) {}

  public record AnswerRow(long questionId, long optionId) {}
}
