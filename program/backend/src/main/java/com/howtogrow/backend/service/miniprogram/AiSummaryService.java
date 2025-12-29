package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.AiSummaryResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.domain.time.AgeInMonthsCalculator;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.ai.AiAssessmentSummaryRepository;
import com.howtogrow.backend.infrastructure.ai.AiClientProvider;
import com.howtogrow.backend.infrastructure.assessment.AssessmentScoreRepository;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentRepository;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.service.common.FixedWindowRateLimiter;
import com.howtogrow.backend.service.common.SubscriptionService;
import com.howtogrow.backend.config.RateLimitProperties;
import java.time.Duration;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSummaryService {
  private final SubscriptionService subscriptionService;
  private final DailyAssessmentRepository assessmentRepo;
  private final ChildRepository childRepo;
  private final AssessmentScoreRepository scoreRepo;
  private final AiAssessmentSummaryRepository summaryRepo;
  private final AiClientProvider aiClientProvider;
  private final BizClock bizClock;
  private final FixedWindowRateLimiter rateLimiter;
  private final RateLimitProperties rateLimitProperties;

  public AiSummaryService(
      SubscriptionService subscriptionService,
      DailyAssessmentRepository assessmentRepo,
      ChildRepository childRepo,
      AssessmentScoreRepository scoreRepo,
      AiAssessmentSummaryRepository summaryRepo,
      AiClientProvider aiClientProvider,
      BizClock bizClock,
      FixedWindowRateLimiter rateLimiter,
      RateLimitProperties rateLimitProperties) {
    this.subscriptionService = subscriptionService;
    this.assessmentRepo = assessmentRepo;
    this.childRepo = childRepo;
    this.scoreRepo = scoreRepo;
    this.summaryRepo = summaryRepo;
    this.aiClientProvider = aiClientProvider;
    this.bizClock = bizClock;
    this.rateLimiter = rateLimiter;
    this.rateLimitProperties = rateLimitProperties;
  }

  @Transactional
  public AiSummaryResponse generate(long userId, long assessmentId) {
    subscriptionService.requireSubscribed(userId);
    rateLimiter.require(
        "ai-summary:user:" + userId,
        Duration.ofDays(1),
        Math.max(0, rateLimitProperties.aiSummaryPerDay()));

    var existing = summaryRepo.findByAssessmentId(assessmentId).orElse(null);
    if (existing != null) {
      throw new AppException(ErrorCode.AI_SUMMARY_ALREADY_GENERATED, "summary already generated");
    }

    var assessment =
        assessmentRepo
            .findById(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "assessment not found"));
    if (assessment.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "forbidden");
    }
    if (assessment.submittedAt() == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "assessment must be submitted");
    }

    var child =
        childRepo.findById(assessment.childId()).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "child not found"));
    var ageMonths = AgeInMonthsCalculator.calculate(child.birthDate(), bizClock.today());

    var dimensionScores = scoreRepo.sumDimensionScores(assessmentId);
    var scoreText =
        dimensionScores.stream()
            .map(s -> CapabilityDimension.displayNameOf(s.dimensionCode()) + ":" + s.score())
            .collect(Collectors.joining("，"));

    var prompt =
        """
        孩子月龄：%d
        今日自测维度得分：%s
        请输出一段<=70字总结。
        """
            .formatted(ageMonths, scoreText);

    var ai = aiClientProvider.get().generateShortSummary(prompt);
    var content = ai.content();
    if (content == null || content.isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI summary empty");
    }
    summaryRepo.insert(assessmentId, userId, content);
    return new AiSummaryResponse(content);
  }
}
