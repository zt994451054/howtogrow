package com.howtogrow.backend.domain.time;

import java.time.LocalDate;
import java.time.Period;

public final class AgeInMonthsCalculator {
  private AgeInMonthsCalculator() {}

  public static int calculate(LocalDate birthDate, LocalDate onDate) {
    if (birthDate == null || onDate == null) {
      throw new IllegalArgumentException("birthDate and onDate must not be null");
    }
    if (onDate.isBefore(birthDate)) {
      throw new IllegalArgumentException("onDate must not be before birthDate");
    }
    Period period = Period.between(birthDate, onDate);
    return period.getYears() * 12 + period.getMonths();
  }
}

