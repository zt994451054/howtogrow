package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.AssessmentDimensionScoreView;
import com.howtogrow.backend.controller.admin.dto.AssessmentView;
import com.howtogrow.backend.controller.admin.dto.PageResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.admin.AssessmentQueryRepository;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class AdminAssessmentService {
  private final AssessmentQueryRepository queryRepo;

  private static final int EXPORT_MAX_ROWS = 10_000;
  private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");
  private static final DateTimeFormatter EXCEL_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(BIZ_ZONE);

  public AdminAssessmentService(AssessmentQueryRepository queryRepo) {
    this.queryRepo = queryRepo;
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
}
