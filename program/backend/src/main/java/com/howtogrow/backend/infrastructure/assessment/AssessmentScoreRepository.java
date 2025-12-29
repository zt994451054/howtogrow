package com.howtogrow.backend.infrastructure.assessment;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AssessmentScoreRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AssessmentScoreRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public long insertAnswer(long assessmentId, long assessmentItemId, long optionId) {
    var sql =
        """
        INSERT INTO daily_assessment_answer(assessment_id, assessment_item_id, option_id, created_at)
        VALUES (:assessmentId, :assessmentItemId, :optionId, NOW(3))
        """;
    var kh = new org.springframework.jdbc.support.GeneratedKeyHolder();
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("assessmentId", assessmentId)
            .addValue("assessmentItemId", assessmentItemId)
            .addValue("optionId", optionId),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to insert daily_assessment_answer");
    }
    return id.longValue();
  }

  public int insertDimensionScoresFromOption(long assessmentId, long assessmentAnswerId, long optionId) {
    var sql =
        """
        INSERT INTO daily_assessment_dimension_score(assessment_id, assessment_answer_id, dimension_code, score, created_at)
        SELECT :assessmentId, :assessmentAnswerId, ods.dimension_code, ods.score, NOW(3)
        FROM option_dimension_score ods
        WHERE ods.option_id = :optionId
        """;
    return jdbc.update(
        sql,
        Map.of(
            "assessmentId", assessmentId,
            "assessmentAnswerId", assessmentAnswerId,
            "optionId", optionId));
  }

  public void deleteAnswersForAssessment(long assessmentId) {
    jdbc.update(
        "DELETE FROM daily_assessment_dimension_score WHERE assessment_id = :assessmentId",
        Map.of("assessmentId", assessmentId));
    jdbc.update(
        "DELETE FROM daily_assessment_answer WHERE assessment_id = :assessmentId",
        Map.of("assessmentId", assessmentId));
  }

  public List<DimensionScoreRow> sumDimensionScores(long assessmentId) {
    var sql =
        """
        SELECT ds.dimension_code, SUM(ds.score) AS total_score
        FROM daily_assessment_dimension_score ds
        WHERE ds.assessment_id = :assessmentId
        GROUP BY ds.dimension_code
        """;
    return jdbc.query(
        sql,
        Map.of("assessmentId", assessmentId),
        (rs, rowNum) ->
            new DimensionScoreRow(
                rs.getString("dimension_code"),
                rs.getLong("total_score")));
  }

  public record DimensionScoreRow(String dimensionCode, long score) {}
}
