package com.howtogrow.backend.infrastructure.parenting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

class DailyParentingDiaryRepositoryTest {
  @Test
  void upsert_existing_allowsNullImageUrl() {
    var jdbc = mock(NamedParameterJdbcTemplate.class);
    when(jdbc.queryForList(anyString(), anyMap(), eq(Long.class))).thenReturn(List.of(1L));
    when(jdbc.update(anyString(), any(SqlParameterSource.class))).thenReturn(1);

    var repo = new DailyParentingDiaryRepository(jdbc);

    assertDoesNotThrow(() -> repo.upsert(1L, 2L, LocalDate.of(2026, 2, 1), "hi", null));

    var paramsCaptor = ArgumentCaptor.forClass(SqlParameterSource.class);
    verify(jdbc).update(contains("UPDATE daily_parenting_diary"), paramsCaptor.capture());

    var params = paramsCaptor.getValue();
    assertTrue(params.hasValue("imageUrl"));
    assertNull(params.getValue("imageUrl"));
  }
}

