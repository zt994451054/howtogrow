package com.howtogrow.backend.infrastructure.aichat;

import com.howtogrow.backend.infrastructure.db.SqlPagination;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AiQuickQuestionRepository {
  private final NamedParameterJdbcTemplate jdbc;

  public AiQuickQuestionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<String> listActivePrompts(int limit) {
    var sql =
        """
        SELECT prompt
        FROM ai_agent_quick_question
        WHERE status = 1 AND deleted_at IS NULL
        ORDER BY sort_no ASC, id DESC
        """
            + SqlPagination.limit(limit);
    return jdbc.queryForList(sql, Map.of(), String.class);
  }
}

