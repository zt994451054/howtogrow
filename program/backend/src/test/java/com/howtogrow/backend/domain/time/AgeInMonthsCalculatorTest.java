package com.howtogrow.backend.domain.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AgeInMonthsCalculatorTest {
  @Test
  void calculatesMonths() {
    var birth = LocalDate.of(2024, 1, 15);
    var on = LocalDate.of(2025, 12, 27);
    assertEquals(23, AgeInMonthsCalculator.calculate(birth, on));
  }

  @Test
  void rejectsOnDateBeforeBirth() {
    var birth = LocalDate.of(2024, 1, 15);
    var on = LocalDate.of(2024, 1, 14);
    assertThrows(IllegalArgumentException.class, () -> AgeInMonthsCalculator.calculate(birth, on));
  }
}

