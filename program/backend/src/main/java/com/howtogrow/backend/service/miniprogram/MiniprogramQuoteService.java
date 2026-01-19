package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.QuoteResponse;
import com.howtogrow.backend.domain.quote.QuoteScene;
import com.howtogrow.backend.domain.time.AgeInYearsCalculator;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.quote.QuoteRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MiniprogramQuoteService {
  private final QuoteRepository quoteRepo;
  private final ChildRepository childRepo;
  private final BizClock bizClock;

  public MiniprogramQuoteService(QuoteRepository quoteRepo, ChildRepository childRepo, BizClock bizClock) {
    this.quoteRepo = quoteRepo;
    this.childRepo = childRepo;
    this.bizClock = bizClock;
  }

  public List<QuoteResponse> random(long userId, long childId, String scene) {
    var normalized =
        QuoteScene.fromValue(scene).orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "场景不合法"));
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    int ageYears = AgeInYearsCalculator.calculate(child.birthDate(), bizClock.today());
    var content = quoteRepo.pickRandomActiveBySceneAndAge(normalized.value(), ageYears).orElse(null);
    if (content == null || content.isBlank()) {
      return List.of();
    }
    return List.of(new QuoteResponse(content));
  }
}
