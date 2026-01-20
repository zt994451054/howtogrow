package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.AdminChildView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.domain.time.AgeInYearsCalculator;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.admin.ChildQueryRepository;
import com.howtogrow.backend.infrastructure.admin.ChildQueryRepository.ChildQuery;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class AdminChildService {
  private static final int MAX_PAGE_SIZE = 200;

  private final ChildQueryRepository queryRepo;
  private final BizClock bizClock;

  public AdminChildService(ChildQueryRepository queryRepo, BizClock bizClock) {
    this.queryRepo = queryRepo;
    this.bizClock = bizClock;
  }

  public PageResponse<AdminChildView> list(
      int page,
      int pageSize,
      Long userId,
      String userNickname,
      Long childId,
      String childNickname,
      Integer gender,
      Integer ageMin,
      Integer ageMax,
      Integer status) {
    int safePageSize = Math.max(1, Math.min(MAX_PAGE_SIZE, pageSize));
    int offset = (page - 1) * safePageSize;

    LocalDate today = bizClock.today();
    var birthDateFrom = resolveBirthDateFromAgeMax(ageMax, today);
    var birthDateTo = resolveBirthDateToAgeMin(ageMin, today);
    if (ageMax != null && birthDateTo == null) {
      // ageMax 筛选语义是 “0 <= ageYear <= ageMax”，因此需要显式排除未来出生日期。
      birthDateTo = today;
    }
    if (birthDateFrom != null && birthDateTo != null && birthDateFrom.isAfter(birthDateTo)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "年龄范围不合法（ageMin 必须 <= ageMax）");
    }

    var query =
        new ChildQuery(userId, userNickname, childId, childNickname, gender, birthDateFrom, birthDateTo, status);
    long total = queryRepo.countChildren(query);
    var items =
        queryRepo.listChildren(query, offset, safePageSize).stream()
            .map(
                c ->
                    new AdminChildView(
                        c.childId(),
                        c.userId(),
                        c.userNickname(),
                        c.userAvatarUrl(),
                        c.childNickname(),
                        c.gender(),
                        c.birthDate(),
                        calculateAgeYearSafe(c.birthDate(), today),
                        c.status(),
                        c.createdAt()))
            .toList();
    return new PageResponse<>(page, safePageSize, total, items);
  }

  private static int calculateAgeYearSafe(LocalDate birthDate, LocalDate today) {
    if (birthDate == null || today == null) return 0;
    if (birthDate.isAfter(today)) return 0;
    return AgeInYearsCalculator.calculate(birthDate, today);
  }

  private static LocalDate resolveBirthDateFromAgeMax(Integer ageMax, LocalDate today) {
    if (ageMax == null) return null;
    // ageYear <= ageMax  <=>  birth_date >= today - (ageMax + 1) years + 1 day
    return today.minusYears((long) ageMax + 1L).plusDays(1);
  }

  private static LocalDate resolveBirthDateToAgeMin(Integer ageMin, LocalDate today) {
    if (ageMin == null) return null;
    // ageYear >= ageMin  <=>  birth_date <= today - ageMin years
    return today.minusYears(ageMin.longValue());
  }
}
