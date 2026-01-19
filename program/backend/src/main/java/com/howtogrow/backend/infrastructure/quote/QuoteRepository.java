package com.howtogrow.backend.infrastructure.quote;

import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class QuoteRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public QuoteRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<String> pickRandomActive() {
    var sql =
        """
        SELECT content
        FROM quote
        WHERE status = 1 AND deleted_at IS NULL
        ORDER BY RAND()
        LIMIT 1
        """;
    var rows = jdbc.queryForList(sql, Map.of(), String.class);
    return rows.stream().findFirst();
  }

  public Optional<String> pickRandomActiveBySceneAndAge(String scene, int ageYear) {
    var sql =
        """
        SELECT content
        FROM quote
        WHERE status = 1
          AND deleted_at IS NULL
          AND scene = :scene
          AND min_age <= :ageYear
          AND max_age >= :ageYear
        ORDER BY RAND()
        LIMIT 1
        """;
    var rows = jdbc.queryForList(sql, Map.of("scene", scene, "ageYear", ageYear), String.class);
    return rows.stream().findFirst();
  }
}
