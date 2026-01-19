package com.howtogrow.backend.infrastructure.child;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class ChildRepository {
  private static final RowMapper<Child> ROW_MAPPER = (rs, rowNum) -> toChild(rs);
  private final NamedParameterJdbcTemplate jdbc;

  public ChildRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<Child> listByUserId(long userId) {
    var sql =
        """
        SELECT id, user_id, nickname, gender, birth_date, parent_identity
        FROM child
        WHERE user_id = :userId AND status = 1 AND deleted_at IS NULL
        ORDER BY id DESC
        """;
    return jdbc.query(sql, Map.of("userId", userId), ROW_MAPPER);
  }

  public Optional<Child> findById(long id) {
    var sql =
        """
        SELECT id, user_id, nickname, gender, birth_date, parent_identity
        FROM child
        WHERE id = :id AND status = 1 AND deleted_at IS NULL
        """;
    var rows = jdbc.query(sql, Map.of("id", id), ROW_MAPPER);
    return rows.stream().findFirst();
  }

  public long create(long userId, String nickname, int gender, LocalDate birthDate, String parentIdentity) {
    KeyHolder kh = new GeneratedKeyHolder();
    var sql =
        """
        INSERT INTO child(user_id, nickname, gender, birth_date, parent_identity, status, created_at, updated_at)
        VALUES (:userId, :nickname, :gender, :birthDate, :parentIdentity, 1, NOW(3), NOW(3))
        """;
    jdbc.update(
        sql,
        new org.springframework.jdbc.core.namedparam.MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("nickname", nickname)
            .addValue("gender", gender)
            .addValue("birthDate", birthDate)
            .addValue("parentIdentity", parentIdentity),
        kh);
    var id = kh.getKey();
    if (id == null) {
      throw new IllegalStateException("failed to create child");
    }
    return id.longValue();
  }

  public void update(
      long childId, long userId, String nickname, int gender, LocalDate birthDate, String parentIdentity) {
    var sql =
        """
        UPDATE child
        SET nickname = :nickname, gender = :gender, birth_date = :birthDate, parent_identity = :parentIdentity, updated_at = NOW(3)
        WHERE id = :childId AND user_id = :userId AND status = 1 AND deleted_at IS NULL
        """;
    jdbc.update(
        sql,
        Map.of(
            "childId", childId,
            "userId", userId,
            "nickname", nickname,
            "gender", gender,
            "birthDate", birthDate,
            "parentIdentity", parentIdentity));
  }

  public void softDelete(long childId, long userId) {
    var sql =
        """
        UPDATE child
        SET status = 0, deleted_at = NOW(3), updated_at = NOW(3)
        WHERE id = :childId AND user_id = :userId AND status = 1 AND deleted_at IS NULL
        """;
    jdbc.update(sql, Map.of("childId", childId, "userId", userId));
  }

  private static Child toChild(ResultSet rs) throws SQLException {
    return new Child(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("nickname"),
        rs.getInt("gender"),
        rs.getObject("birth_date", LocalDate.class),
        rs.getString("parent_identity"));
  }
}
