package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.AssessmentDetailView;
import com.howtogrow.backend.controller.admin.dto.AssessmentDimensionScoreView;
import com.howtogrow.backend.controller.admin.dto.AssessmentView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.admin.AdminAssessmentDetailQueryRepository;
import com.howtogrow.backend.infrastructure.admin.AssessmentQueryRepository;
import com.howtogrow.backend.infrastructure.assessment.DailyAssessmentHistoryRepository;
import com.howtogrow.backend.infrastructure.question.QuestionSnapshotViewRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class AdminAssessmentService {
  private final AssessmentQueryRepository queryRepo;
  private final AdminAssessmentDetailQueryRepository detailQueryRepo;
  private final DailyAssessmentHistoryRepository historyRepo;
  private final QuestionSnapshotViewRepository questionViewRepo;

  private static final int EXPORT_MAX_ROWS = 10_000;
  private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
  private static final DateTimeFormatter EXCEL_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(BIZ_ZONE);

  public AdminAssessmentService(
      AssessmentQueryRepository queryRepo,
      AdminAssessmentDetailQueryRepository detailQueryRepo,
      DailyAssessmentHistoryRepository historyRepo,
      QuestionSnapshotViewRepository questionViewRepo) {
    this.queryRepo = queryRepo;
    this.detailQueryRepo = detailQueryRepo;
    this.historyRepo = historyRepo;
    this.questionViewRepo = questionViewRepo;
  }

  public PageResponse<AssessmentView> list(
      int page,
      int pageSize,
      Long userId,
      Long childId,
      String keyword,
      LocalDate bizDateFrom,
      LocalDate bizDateTo) {
    int offset = (page - 1) * pageSize;
    long total = queryRepo.countAssessments(userId, childId, keyword, bizDateFrom, bizDateTo);
    var rows =
        queryRepo.listAssessments(offset, pageSize, userId, childId, keyword, bizDateFrom, bizDateTo);
    var items = toViews(rows);
    return new PageResponse<>(page, pageSize, total, items);
  }

  public byte[] exportExcel(
      Long userId,
      Long childId,
      String keyword,
      LocalDate bizDateFrom,
      LocalDate bizDateTo) {
    if (bizDateFrom != null && bizDateTo != null && bizDateFrom.isAfter(bizDateTo)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "开始日期不能晚于结束日期");
    }

    var rows =
        queryRepo.listAssessments(0, EXPORT_MAX_ROWS + 1, userId, childId, keyword, bizDateFrom, bizDateTo);
    if (rows.size() > EXPORT_MAX_ROWS) {
      throw new AppException(
          ErrorCode.INVALID_REQUEST, "导出数量超过 " + EXPORT_MAX_ROWS + " 条，请缩小筛选范围后重试");
    }

    var items = toViews(rows);
    try (var workbook = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("自测记录");

      int r = 0;
      var header =
          List.of(
              "自测ID",
              "日期",
              "用户ID",
              "用户昵称",
              "孩子ID",
              "孩子昵称",
              "情绪",
              "沟通",
              "规则",
              "关系",
              "学习",
              "提交时间");
      var headerRow = sheet.createRow(r++);
      for (int i = 0; i < header.size(); i++) {
        headerRow.createCell(i).setCellValue(header.get(i));
      }

      for (var a : items) {
        var row = sheet.createRow(r++);
        int c = 0;
        row.createCell(c++).setCellValue(a.assessmentId());
        row.createCell(c++).setCellValue(a.bizDate() == null ? "" : a.bizDate().toString());
        row.createCell(c++).setCellValue(a.userId());
        row.createCell(c++).setCellValue(a.userNickname() == null ? "" : a.userNickname());
        row.createCell(c++).setCellValue(a.childId());
        row.createCell(c++).setCellValue(a.childNickname() == null ? "" : a.childNickname());
        row.createCell(c++).setCellValue(a.emotionManagementScore());
        row.createCell(c++).setCellValue(a.communicationExpressionScore());
        row.createCell(c++).setCellValue(a.ruleGuidanceScore());
        row.createCell(c++).setCellValue(a.relationshipBuildingScore());
        row.createCell(c++).setCellValue(a.learningSupportScore());
        row.createCell(c++).setCellValue(formatInstant(a.submittedAt()));
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "生成 Excel 失败");
    }
  }

  public AssessmentDetailView detail(long assessmentId) {
    var base =
        detailQueryRepo
            .findByAssessmentId(assessmentId)
            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "自测不存在"));
    var dimScores = listDimensionScores(base.assessmentId());
    var items = buildAssessmentItems(base.assessmentId());
    return new AssessmentDetailView(
        base.assessmentId(),
        base.userId(),
        base.userNickname(),
        base.userAvatarUrl(),
        base.childId(),
        base.childNickname(),
        base.bizDate(),
        base.submittedAt(),
        base.aiSummary(),
        dimScores,
        items);
  }

  public byte[] exportWord(long assessmentId) {
    var detail = detail(assessmentId);
    try (var doc = new XWPFDocument(); var out = new ByteArrayOutputStream()) {
      buildWordDoc(doc, detail);
      doc.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new AppException(ErrorCode.INTERNAL_ERROR, "生成 Word 失败");
    }
  }

  private List<AssessmentDimensionScoreView> listDimensionScores(long assessmentId) {
    var rows = queryRepo.listDimensionScoresByAssessmentIds(List.of(assessmentId));
    Map<String, Long> byCode = new HashMap<>();
    for (var r : rows) {
      byCode.put(r.dimensionCode(), r.score());
    }
    return buildDimensionScores(byCode);
  }

  private List<AssessmentDetailView.ItemView> buildAssessmentItems(long assessmentId) {
    var items = historyRepo.listItems(assessmentId);
    var questionIds =
        items.stream().map(DailyAssessmentHistoryRepository.ItemRow::questionId).distinct().toList();

    var questionOptionRows = questionViewRepo.listQuestionOptionRows(questionIds);
    var questionInfo = new HashMap<Long, QuestionInfo>();
    for (var row : questionOptionRows) {
      var info =
          questionInfo.computeIfAbsent(
              row.questionId(), (qid) -> new QuestionInfo(row.questionContent(), row.questionType(), new ArrayList<>()));
      info
          .options()
          .add(
              new AssessmentDetailView.OptionView(
                  row.optionId(),
                  row.optionContent(),
                  row.suggestFlag(),
                  row.improvementTip(),
                  row.optionSortNo()));
    }

    var answers = historyRepo.listAnswers(assessmentId);
    Map<Long, List<Long>> selectedMap = new HashMap<>();
    for (var a : answers) {
      selectedMap.computeIfAbsent(a.questionId(), (k) -> new ArrayList<>()).add(a.optionId());
    }

    List<AssessmentDetailView.ItemView> out = new ArrayList<>();
    for (var it : items) {
      var info = questionInfo.get(it.questionId());
      if (info == null) {
        out.add(
            new AssessmentDetailView.ItemView(
                it.displayOrder(),
                it.questionId(),
                "SINGLE",
                "（题目已不存在）",
                List.of(),
                List.copyOf(selectedMap.getOrDefault(it.questionId(), List.of()))));
        continue;
      }

      out.add(
          new AssessmentDetailView.ItemView(
              it.displayOrder(),
              it.questionId(),
              normalizeQuestionType(info.questionType()),
              normalizeQuestionContent(info.content()),
              List.copyOf(info.options()),
              List.copyOf(selectedMap.getOrDefault(it.questionId(), List.of()))));
    }
    return out;
  }

  private static String normalizeQuestionType(String questionType) {
    if (questionType == null || questionType.isBlank()) return "SINGLE";
    return questionType;
  }

  private static String normalizeQuestionContent(String content) {
    if (content == null || content.isBlank()) return "（题目已不存在）";
    return content;
  }

  private static void buildWordDoc(XWPFDocument doc, AssessmentDetailView detail) {
    var title = doc.createParagraph();
    title.setAlignment(ParagraphAlignment.CENTER);
    var titleRun = title.createRun();
    titleRun.setBold(true);
    titleRun.setFontSize(16);
    titleRun.setText("自测结果报告");

    var infoTable = doc.createTable(5, 2);
    infoTable.getRow(0).getCell(0).setText("自测ID");
    infoTable.getRow(0).getCell(1).setText(String.valueOf(detail.assessmentId()));
    infoTable.getRow(1).getCell(0).setText("日期");
    infoTable.getRow(1).getCell(1).setText(detail.bizDate() == null ? "" : detail.bizDate().toString());
    infoTable.getRow(2).getCell(0).setText("提交时间");
    infoTable.getRow(2).getCell(1).setText(formatInstant(detail.submittedAt()));
    infoTable.getRow(3).getCell(0).setText("用户");
    infoTable.getRow(3).getCell(1).setText(formatUserLabel(detail.userId(), detail.userNickname()));
    infoTable.getRow(4).getCell(0).setText("孩子");
    infoTable.getRow(4).getCell(1).setText(formatChildLabel(detail.childId(), detail.childNickname()));

    addSectionHeader(doc, "维度得分");
    var dimScores = detail.dimensionScores() == null ? List.<AssessmentDimensionScoreView>of() : detail.dimensionScores();
    var dimTable = doc.createTable(dimScores.size() + 1, 2);
    dimTable.getRow(0).getCell(0).setText("维度");
    dimTable.getRow(0).getCell(1).setText("得分");
    for (int i = 0; i < dimScores.size(); i++) {
      var row = dimTable.getRow(i + 1);
      var d = dimScores.get(i);
      row.getCell(0).setText(d == null ? "" : safe(d.dimensionName()));
      row.getCell(1).setText(d == null ? "" : String.valueOf(d.score()));
    }

    if (detail.aiSummary() != null && !detail.aiSummary().isBlank()) {
      addSectionHeader(doc, "AI 总结");
      var p = doc.createParagraph();
      p.createRun().setText(detail.aiSummary());
    }

    addSectionHeader(doc, "题目与作答");
    var items = detail.items() == null ? List.<AssessmentDetailView.ItemView>of() : detail.items();
    for (var it : items) {
      if (it == null) continue;
      var q = doc.createParagraph();
      var qRun = q.createRun();
      qRun.setBold(true);
      qRun.setText("Q" + it.displayOrder() + ". ");
      q.createRun().setText(safe(it.questionContent()));

      var selectedIds = it.selectedOptionIds() == null ? List.<Long>of() : it.selectedOptionIds();
      if (selectedIds.isEmpty()) {
        var p = doc.createParagraph();
        p.setIndentationLeft(360);
        p.createRun().setText("（未记录作答）");
        continue;
      }

      var optMap = toOptionMap(it.options());
      for (var optionId : selectedIds) {
        var opt = optionId == null ? null : optMap.get(optionId);
        var p = doc.createParagraph();
        p.setIndentationLeft(360);
        p.createRun().setText("· " + formatSelectedOption(optionId, opt));

        if (opt != null && opt.suggestFlag() == 0 && opt.improvementTip() != null && !opt.improvementTip().isBlank()) {
          var tip = doc.createParagraph();
          tip.setIndentationLeft(720);
          tip.createRun().setText("改进建议：" + opt.improvementTip());
        }
      }
    }
  }

  private static void addSectionHeader(XWPFDocument doc, String title) {
    var p = doc.createParagraph();
    p.setSpacingBefore(240);
    var run = p.createRun();
    run.setBold(true);
    run.setFontSize(13);
    run.setText(title);
  }

  private static Map<Long, AssessmentDetailView.OptionView> toOptionMap(List<AssessmentDetailView.OptionView> options) {
    Map<Long, AssessmentDetailView.OptionView> map = new HashMap<>();
    if (options == null) return map;
    for (var o : options) {
      if (o == null) continue;
      map.put(o.optionId(), o);
    }
    return map;
  }

  private static String formatSelectedOption(Long optionId, AssessmentDetailView.OptionView opt) {
    if (opt == null) {
      return "（选项 ID: " + (optionId == null ? "-" : optionId) + "）";
    }
    String flag = opt.suggestFlag() == 1 ? "建议" : "不建议";
    return safe(opt.content()) + "（" + flag + "）";
  }

  private static String formatUserLabel(long userId, String nickname) {
    String name = nickname == null || nickname.isBlank() ? "用户" : nickname;
    return name + "（ID: " + userId + "）";
  }

  private static String formatChildLabel(long childId, String nickname) {
    String name = nickname == null || nickname.isBlank() ? "（未知）" : nickname;
    return name + "（ID: " + childId + "）";
  }

  private static String safe(String v) {
    if (v == null) return "";
    return v;
  }

  private List<AssessmentView> toViews(List<AssessmentQueryRepository.AssessmentRow> rows) {
    var ids = rows.stream().map(AssessmentQueryRepository.AssessmentRow::id).toList();
    var scoreRows = queryRepo.listDimensionScoresByAssessmentIds(ids);
    Map<Long, Map<String, Long>> scoreMap = new HashMap<>();
    for (var r : scoreRows) {
      scoreMap.computeIfAbsent(r.assessmentId(), (k) -> new HashMap<>()).put(r.dimensionCode(), r.score());
    }

    return rows.stream()
        .map(
            a -> {
              var dimScores = buildDimensionScores(scoreMap.get(a.id()));
              var dimScoreByCode = toScoreMap(dimScores);
              return new AssessmentView(
                  a.id(),
                  a.userId(),
                  a.userNickname(),
                  a.userAvatarUrl(),
                  a.childId(),
                  a.childNickname(),
                  a.bizDate(),
                  a.submittedAt(),
                  dimScoreByCode.getOrDefault(CapabilityDimension.EMOTION_MANAGEMENT.code(), 0L),
                  dimScoreByCode.getOrDefault(CapabilityDimension.COMMUNICATION_EXPRESSION.code(), 0L),
                  dimScoreByCode.getOrDefault(CapabilityDimension.RULE_GUIDANCE.code(), 0L),
                  dimScoreByCode.getOrDefault(CapabilityDimension.RELATIONSHIP_BUILDING.code(), 0L),
                  dimScoreByCode.getOrDefault(CapabilityDimension.LEARNING_SUPPORT.code(), 0L),
                  dimScores);
            })
        .toList();
  }

  private static List<AssessmentDimensionScoreView> buildDimensionScores(Map<String, Long> byCode) {
    var out = new ArrayList<AssessmentDimensionScoreView>();
    for (var dim : CapabilityDimension.ordered()) {
      long score = 0L;
      if (byCode != null) {
        var v = byCode.get(dim.code());
        if (v != null) score = v;
      }
      out.add(new AssessmentDimensionScoreView(dim.code(), dim.displayName(), score));
    }
    return out;
  }

  private static Map<String, Long> toScoreMap(List<AssessmentDimensionScoreView> dims) {
    Map<String, Long> map = new HashMap<>();
    if (dims == null) return map;
    for (var d : dims) {
      if (d == null || d.dimensionCode() == null) continue;
      map.put(d.dimensionCode(), d.score());
    }
    return map;
  }

  private static String formatInstant(Instant instant) {
    if (instant == null) return "";
    return EXCEL_TIME.format(instant);
  }

  private record QuestionInfo(String content, String questionType, List<AssessmentDetailView.OptionView> options) {}
}
