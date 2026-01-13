package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.AiSummaryResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.domain.time.AgeInMonthsCalculator;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.ai.AiAssessmentSummaryRepository;
import com.howtogrow.backend.infrastructure.ai.AiClient;
import com.howtogrow.backend.infrastructure.assessment.AssessmentScoreRepository;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentHistoryRepository;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentRepository;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.question.QuestionSnapshotViewRepository;
import com.howtogrow.backend.service.common.FixedWindowRateLimiter;
import com.howtogrow.backend.service.common.SubscriptionService;
import com.howtogrow.backend.config.RateLimitProperties;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSummaryService {
  private final SubscriptionService subscriptionService;
  private final DailyAssessmentRepository assessmentRepo;
  private final ChildRepository childRepo;
  private final AssessmentScoreRepository scoreRepo;
  private final DailyAssessmentHistoryRepository historyRepo;
  private final QuestionSnapshotViewRepository questionSnapshotViewRepo;
  private final AiAssessmentSummaryRepository summaryRepo;
  private final AiClient aiClient;
  private final BizClock bizClock;
  private final FixedWindowRateLimiter rateLimiter;
  private final RateLimitProperties rateLimitProperties;

  public AiSummaryService(
      SubscriptionService subscriptionService,
      DailyAssessmentRepository assessmentRepo,
      ChildRepository childRepo,
      AssessmentScoreRepository scoreRepo,
      DailyAssessmentHistoryRepository historyRepo,
      QuestionSnapshotViewRepository questionSnapshotViewRepo,
      AiAssessmentSummaryRepository summaryRepo,
      AiClient aiClient,
      BizClock bizClock,
      FixedWindowRateLimiter rateLimiter,
      RateLimitProperties rateLimitProperties) {
    this.subscriptionService = subscriptionService;
    this.assessmentRepo = assessmentRepo;
    this.childRepo = childRepo;
    this.scoreRepo = scoreRepo;
    this.historyRepo = historyRepo;
    this.questionSnapshotViewRepo = questionSnapshotViewRepo;
    this.summaryRepo = summaryRepo;
    this.aiClient = aiClient;
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
      throw new AppException(ErrorCode.AI_SUMMARY_ALREADY_GENERATED, "AI 总结已生成");
    }

    var assessment =
        assessmentRepo
            .findById(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "测评不存在"));
    if (assessment.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    if (assessment.submittedAt() == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "测评尚未提交");
    }

    var child =
        childRepo.findById(assessment.childId()).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    var ageMonths = AgeInMonthsCalculator.calculate(child.birthDate(), bizClock.today());

    var dimensionScores = scoreRepo.sumDimensionScores(assessmentId);
    var scoreText =
        dimensionScores.stream()
            .map(s -> CapabilityDimension.displayNameOf(s.dimensionCode()) + ":" + s.score())
            .collect(Collectors.joining("，"));

    var flowText = buildAssessmentFlowText(assessmentId);
    var prompt =
        """
        孩子月龄：%d
        今日自测维度得分：%s
        
        本次自测过程（问题 / 你的选择 / 对应建议）：
        %s
        
        请基于以上测评过程输出一段<=200字总结，风格：共情 + 1条可执行建议，不做诊断、不贴标签。
        """
            .formatted(ageMonths, scoreText, flowText);

    var ai = aiClient.generateShortSummary(prompt);
    var content = ai.content();
    if (content == null || content.isBlank()) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "AI 总结生成失败");
    }
    summaryRepo.insert(assessmentId, userId, content);
    return new AiSummaryResponse(content);
  }

  private String buildAssessmentFlowText(long assessmentId) {
    var items = historyRepo.listItems(assessmentId);
    if (items == null || items.isEmpty()) {
      return "（无题目记录）";
    }

    var questionIds =
        items.stream().map(DailyAssessmentHistoryRepository.ItemRow::questionId).distinct().toList();
    var optionRows = questionSnapshotViewRepo.listQuestionOptionRows(questionIds);

    var questions = new HashMap<Long, QuestionInfo>();
    for (var row : optionRows) {
      var info =
          questions.computeIfAbsent(
              row.questionId(),
              (qid) -> new QuestionInfo(row.questionContent(), row.questionType(), new HashMap<>()));
      info.optionsById.put(
          row.optionId(),
          new OptionInfo(row.optionContent(), row.suggestFlag(), row.improvementTip()));
    }

    var answers = historyRepo.listAnswers(assessmentId);
    Map<Long, List<Long>> answerMap = new HashMap<>();
    for (var a : answers) {
      answerMap.computeIfAbsent(a.questionId(), (k) -> new ArrayList<>()).add(a.optionId());
    }

    var sb = new StringBuilder();
    items.stream()
        .sorted(
            (a, b) -> {
              var c = Integer.compare(a.displayOrder(), b.displayOrder());
              if (c != 0) return c;
              return Long.compare(a.itemId(), b.itemId());
            })
        .forEach(
            it -> {
              var q = questions.get(it.questionId());
              var qTitle = q == null || q.content == null || q.content.isBlank() ? "（题目已不存在）" : q.content;
              sb.append(it.displayOrder()).append(". ").append(qTitle.replace("\n", " ")).append("\n");

              var picked = answerMap.getOrDefault(it.questionId(), List.of());
              if (picked.isEmpty()) {
                sb.append("   - 你的选择：无\n");
                return;
              }
              for (var optionId : picked) {
                OptionInfo opt = q == null ? null : q.optionsById.get(optionId);
                var optText = opt == null || opt.content == null || opt.content.isBlank() ? "（选项已不存在）" : opt.content;
                var flagText = opt != null && opt.suggestFlag == 0 ? "不建议" : "建议";
                sb.append("   - 你的选择：").append(optText).append("（").append(flagText).append("）\n");
                if (opt != null && opt.improvementTip != null && !opt.improvementTip.isBlank()) {
                  sb.append("     建议：").append(opt.improvementTip.trim()).append("\n");
                }
              }
            });

    return sb.toString().trim();
  }

  private static final class QuestionInfo {
    private final String content;
    private final String questionType;
    private final Map<Long, OptionInfo> optionsById;

    private QuestionInfo(String content, String questionType, Map<Long, OptionInfo> optionsById) {
      this.content = content;
      this.questionType = questionType;
      this.optionsById = optionsById;
    }
  }

  private record OptionInfo(String content, int suggestFlag, String improvementTip) {}
}
