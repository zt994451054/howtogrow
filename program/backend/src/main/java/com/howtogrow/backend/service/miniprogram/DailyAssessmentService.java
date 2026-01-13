package com.howtogrow.backend.service.miniprogram;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentAnswerRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentBeginResponse;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentItemView;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentReplaceResponse;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentSubmitRequest;
import com.howtogrow.backend.controller.miniprogram.dto.DailyAssessmentSubmitResponse;
import com.howtogrow.backend.controller.miniprogram.dto.DimensionScoreView;
import com.howtogrow.backend.controller.miniprogram.dto.QuestionOptionView;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.domain.time.AgeInYearsCalculator;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.assessment.AssessmentScoreRepository;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentItemRepository;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentRepository;
import com.howtogrow.backend.infrastructure.assessment.session.DailyAssessmentSession;
import com.howtogrow.backend.infrastructure.assessment.session.DailyAssessmentSessionStore;
import com.howtogrow.backend.infrastructure.child.Child;
import com.howtogrow.backend.infrastructure.child.ChildRepository;
import com.howtogrow.backend.infrastructure.question.QuestionOptionRepository;
import com.howtogrow.backend.infrastructure.question.QuestionRepository;
import com.howtogrow.backend.infrastructure.question.QuestionViewRepository;
import com.howtogrow.backend.service.common.EntitlementService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyAssessmentService {
  private static final int DAILY_QUESTION_COUNT = 5;
  private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

  private final BizClock bizClock;
  private final Clock clock;
  private final ChildRepository childRepo;
  private final QuestionRepository questionRepo;
  private final QuestionViewRepository questionViewRepo;
  private final QuestionOptionRepository optionRepo;
  private final DailyAssessmentRepository assessmentRepo;
  private final DailyAssessmentItemRepository itemRepo;
  private final AssessmentScoreRepository scoreRepo;
  private final DailyAssessmentSessionStore sessionStore;
  private final EntitlementService entitlementService;

  public DailyAssessmentService(
      BizClock bizClock,
      Clock clock,
      ChildRepository childRepo,
      QuestionRepository questionRepo,
      QuestionViewRepository questionViewRepo,
      QuestionOptionRepository optionRepo,
      DailyAssessmentRepository assessmentRepo,
      DailyAssessmentItemRepository itemRepo,
      AssessmentScoreRepository scoreRepo,
      DailyAssessmentSessionStore sessionStore,
      EntitlementService entitlementService) {
    this.bizClock = bizClock;
    this.clock = clock;
    this.childRepo = childRepo;
    this.questionRepo = questionRepo;
    this.questionViewRepo = questionViewRepo;
    this.optionRepo = optionRepo;
    this.assessmentRepo = assessmentRepo;
    this.itemRepo = itemRepo;
    this.scoreRepo = scoreRepo;
    this.sessionStore = sessionStore;
    this.entitlementService = entitlementService;
  }

  public DailyAssessmentBeginResponse begin(long userId, long childId) {
    entitlementService.requireCanStartDailyAssessment(userId);
    var child = requireChildOwnedByUser(userId, childId);

    var submittedAt = Instant.now(clock);
    if (hasSubmittedToday(userId, childId, submittedAt)) {
      throw new AppException(ErrorCode.DAILY_ASSESSMENT_ALREADY_SUBMITTED, "今日测评已提交");
    }

    int ageYears = AgeInYearsCalculator.calculate(child.birthDate(), bizClock.today());
    var questionIds = questionRepo.pickRandomQuestionIds(ageYears, DAILY_QUESTION_COUNT);
    if (questionIds.size() < DAILY_QUESTION_COUNT) {
      throw new AppException(ErrorCode.QUESTION_POOL_EXHAUSTED, "题库题目不足");
    }

    String sessionId = String.valueOf(submittedAt.toEpochMilli());
    var session =
        new DailyAssessmentSession(userId, childId, List.copyOf(questionIds), new HashSet<>(questionIds));
    sessionStore.save(session, sessionId);

    return new DailyAssessmentBeginResponse(sessionId, loadItems(questionIds));
  }

  public DailyAssessmentReplaceResponse replace(long userId, String sessionId, long childId, int displayOrder) {
    if (displayOrder < 1 || displayOrder > DAILY_QUESTION_COUNT) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "题目序号不合法");
    }
    var child = requireChildOwnedByUser(userId, childId);
    var session = requireSession(userId, childId, sessionId);

    int ageYears = AgeInYearsCalculator.calculate(child.birthDate(), bizClock.today());
    var excluded = session.servedQuestionIds() == null ? Set.<Long>of() : session.servedQuestionIds();
    var newQuestionId =
        questionRepo
            .pickRandomQuestionIdExcluding(ageYears, List.copyOf(excluded))
            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_POOL_EXHAUSTED, "没有可更换的问题了"));

    var updatedIds = new ArrayList<>(session.questionIdsByOrder());
    updatedIds.set(displayOrder - 1, newQuestionId);

    var updatedServed = new HashSet<>(excluded);
    updatedServed.add(newQuestionId);
    sessionStore.save(new DailyAssessmentSession(userId, childId, List.copyOf(updatedIds), updatedServed), sessionId);

    return new DailyAssessmentReplaceResponse(displayOrder, loadItem(newQuestionId, displayOrder));
  }

  @Transactional
  public DailyAssessmentSubmitResponse submit(long userId, String sessionId, DailyAssessmentSubmitRequest request) {
    requireChildOwnedByUser(userId, request.childId());
    var session = requireSession(userId, request.childId(), sessionId);

    var questionIdsByOrder = session.questionIdsByOrder();
    if (questionIdsByOrder == null || questionIdsByOrder.size() != DAILY_QUESTION_COUNT) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "会话题目异常");
    }

    var answers = request.answers();
    if (answers == null || answers.size() != DAILY_QUESTION_COUNT) {
      throw new AppException(ErrorCode.DAILY_ASSESSMENT_INCOMPLETE, "请完成 5 道题目后再提交");
    }

    var answerMap =
        answers.stream().collect(Collectors.toMap(DailyAssessmentAnswerRequest::questionId, a -> a, (a, b) -> b));
    if (answerMap.size() != DAILY_QUESTION_COUNT) {
      throw new AppException(ErrorCode.DAILY_ASSESSMENT_INCOMPLETE, "请完成 5 道不同题目后再提交");
    }

    var sessionQuestionSet = new HashSet<>(questionIdsByOrder);
    if (!answerMap.keySet().equals(sessionQuestionSet)) {
      throw new AppException(ErrorCode.DAILY_ASSESSMENT_INCOMPLETE, "提交答案与当前题目不匹配");
    }

    for (var questionId : questionIdsByOrder) {
      validateAnswer(questionId, answerMap.get(questionId));
    }

    var submittedAt = Instant.now(clock);
    if (hasSubmittedToday(userId, request.childId(), submittedAt)) {
      throw new AppException(ErrorCode.DAILY_ASSESSMENT_ALREADY_SUBMITTED, "今日测评已提交");
    }

    long assessmentId = assessmentRepo.insertSubmitted(userId, request.childId(), submittedAt);

    var itemIdByQuestionId = new HashMap<Long, Long>();
    for (int i = 0; i < questionIdsByOrder.size(); i++) {
      var qid = questionIdsByOrder.get(i);
      var itemId = itemRepo.insertItem(assessmentId, qid, i + 1);
      itemIdByQuestionId.put(qid, itemId);
    }

    for (var questionId : questionIdsByOrder) {
      var itemId = itemIdByQuestionId.get(questionId);
      if (itemId == null) {
        throw new AppException(ErrorCode.INTERNAL_ERROR, "服务异常");
      }
      persistAnswer(assessmentId, itemId, questionId, answerMap.get(questionId));
    }

    entitlementService.onDailyAssessmentSubmitted(userId);
    sessionStore.delete(userId, request.childId(), sessionId);

    var rows = scoreRepo.sumDimensionScores(assessmentId);
    var dimensionScores =
        rows.stream()
            .sorted(Comparator.comparingInt(r -> CapabilityDimension.sortNoOf(r.dimensionCode())))
            .map(
                r ->
                    new DimensionScoreView(
                        r.dimensionCode(), CapabilityDimension.displayNameOf(r.dimensionCode()), r.score()))
            .toList();
    return new DailyAssessmentSubmitResponse(assessmentId, dimensionScores);
  }

  private boolean hasSubmittedToday(long userId, long childId, Instant now) {
    var day = LocalDate.ofInstant(now, BIZ_ZONE);
    var dayStart = day.atStartOfDay(BIZ_ZONE).toInstant();
    var nextDayStart = day.plusDays(1).atStartOfDay(BIZ_ZONE).toInstant();
    return assessmentRepo.existsSubmittedBetween(userId, childId, dayStart, nextDayStart);
  }

  private void validateAnswer(long questionId, DailyAssessmentAnswerRequest answer) {
    if (answer == null) {
      throw new AppException(ErrorCode.DAILY_ASSESSMENT_INCOMPLETE, "缺少答案");
    }
    var typeRow =
        questionRepo
            .findQuestionType(questionId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "题目不存在"));

    var optionIds = dedupeOptionIds(answer.optionIds());
    if ("SINGLE".equalsIgnoreCase(typeRow.questionType()) && optionIds.size() != 1) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "单选题必须且只能选择 1 个选项");
    }
    if (!optionRepo.optionsBelongToQuestion(questionId, optionIds)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "选项不属于该题目");
    }
  }

  private void persistAnswer(
      long assessmentId, long assessmentItemId, long questionId, DailyAssessmentAnswerRequest answer) {
    var optionIds = dedupeOptionIds(answer.optionIds());
    for (var optionId : optionIds) {
      var answerId = scoreRepo.insertAnswer(assessmentId, assessmentItemId, optionId);
      scoreRepo.insertDimensionScoresFromOption(assessmentId, answerId, optionId);
    }
  }

  private static List<Long> dedupeOptionIds(List<Long> optionIds) {
    var seen = new HashSet<Long>();
    var out = new ArrayList<Long>(optionIds.size());
    for (var id : optionIds) {
      if (id != null && seen.add(id)) {
        out.add(id);
      }
    }
    return out;
  }

  private List<DailyAssessmentItemView> loadItems(List<Long> questionIdsByOrder) {
    var rows = questionViewRepo.listActiveQuestionOptionRows(questionIdsByOrder);
    var byQuestionId = new HashMap<Long, QuestionAccumulator>();
    for (var row : rows) {
      byQuestionId.computeIfAbsent(
          row.questionId(),
          id -> new QuestionAccumulator(row.questionId(), row.questionContent(), row.questionType()));
      byQuestionId
          .get(row.questionId())
          .options
          .add(
              new QuestionOptionView(
                  row.optionId(),
                  row.optionContent(),
                  row.optionSortNo(),
                  row.suggestFlag(),
                  row.improvementTip()));
    }

    var out = new ArrayList<DailyAssessmentItemView>(questionIdsByOrder.size());
    for (int i = 0; i < questionIdsByOrder.size(); i++) {
      var questionId = questionIdsByOrder.get(i);
      var acc = byQuestionId.get(questionId);
      if (acc == null) {
        throw new AppException(ErrorCode.INTERNAL_ERROR, "服务异常");
      }
      out.add(new DailyAssessmentItemView(i + 1, questionId, acc.content, acc.questionType, acc.options));
    }
    return out;
  }

  private DailyAssessmentItemView loadItem(long questionId, int displayOrder) {
    var rows = questionViewRepo.listActiveQuestionOptionRows(List.of(questionId));
    if (rows.isEmpty()) {
      throw new AppException(ErrorCode.NOT_FOUND, "题目不存在");
    }
    var first = rows.get(0);
    var options =
        rows.stream()
            .map(
                r ->
                    new QuestionOptionView(
                        r.optionId(),
                        r.optionContent(),
                        r.optionSortNo(),
                        r.suggestFlag(),
                        r.improvementTip()))
            .toList();
    return new DailyAssessmentItemView(displayOrder, questionId, first.questionContent(), first.questionType(), options);
  }

  private DailyAssessmentSession requireSession(long userId, long childId, String sessionId) {
    if (sessionId == null || sessionId.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "会话ID不合法");
    }
    var session =
        sessionStore
            .find(userId, childId, sessionId)
            .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST, "会话已过期"));
    if (session.userId() != userId || session.childId() != childId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    return session;
  }

  private Child requireChildOwnedByUser(long userId, long childId) {
    var child =
        childRepo.findById(childId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "孩子不存在"));
    if (child.userId() != userId) {
      throw new AppException(ErrorCode.FORBIDDEN_RESOURCE, "无权限");
    }
    return child;
  }

  private static final class QuestionAccumulator {
    private final long questionId;
    private final String content;
    private final String questionType;
    private final List<QuestionOptionView> options = new ArrayList<>();

    private QuestionAccumulator(long questionId, String content, String questionType) {
      this.questionId = questionId;
      this.content = content;
      this.questionType = questionType;
    }
  }
}
