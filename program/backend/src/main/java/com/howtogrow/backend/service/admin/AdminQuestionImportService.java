package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.QuestionImportResponse;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.admin.QuestionAdminRepository;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminQuestionImportService {
  private final QuestionAdminRepository questionRepo;

  public AdminQuestionImportService(QuestionAdminRepository questionRepo) {
    this.questionRepo = questionRepo;
  }

  public QuestionImportResponse importExcel(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "file is required");
    }
    try (InputStream in = file.getInputStream();
        var workbook = WorkbookFactory.create(in)) {
      var sheet = workbook.getSheetAt(0);
      if (sheet == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "empty excel");
      }

      var headerRow = sheet.getRow(sheet.getFirstRowNum());
      if (headerRow == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "missing header row");
      }

      var headerIndex = parseHeaderIndex(headerRow);
      var parseResult = parseRows(sheet, headerIndex);
      var parsedRows = parseResult.rows;

      var failures = new ArrayList<QuestionImportResponse.Failure>();
      int total = parsedRows.size();
      int success = 0;

      var groups = groupByQuestion(parsedRows);
      for (var group : groups.values()) {
        try {
          importOneQuestionGroup(group, parseResult.generateSortNo);
          success += group.size();
        } catch (AppException e) {
          for (var r : group) {
            failures.add(new QuestionImportResponse.Failure(r.rowNum, e.getMessage()));
          }
        } catch (Exception e) {
          for (var r : group) {
            failures.add(new QuestionImportResponse.Failure(r.rowNum, "import failed"));
          }
        }
      }

      int failed = total - success;
      return new QuestionImportResponse(total, success, failed, failures);
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "invalid excel file");
    }
  }

  private static Map<String, Integer> parseHeaderIndex(Row headerRow) {
    var map = new HashMap<String, Integer>();
    for (int i = 0; i < headerRow.getLastCellNum(); i++) {
      var cell = headerRow.getCell(i);
      var text = cellText(cell);
      if (!text.isBlank()) {
        map.put(text.trim().toLowerCase(Locale.ROOT), i);
      }
    }
    return map;
  }

  private static ParseResult parseRows(
      org.apache.poi.ss.usermodel.Sheet sheet, Map<String, Integer> headerIndex) {
    if (isChineseTemplate(headerIndex)) {
      return new ParseResult(parseRowsChineseTemplate(sheet, headerIndex), true);
    }
    return new ParseResult(parseRowsLegacy(sheet, headerIndex), false);
  }

  private static boolean isChineseTemplate(Map<String, Integer> headerIndex) {
    return headerIndex.containsKey(keyOf("问题")) && headerIndex.containsKey(keyOf("选项值"));
  }

  private static List<ImportRow> parseRowsLegacy(
      org.apache.poi.ss.usermodel.Sheet sheet, Map<String, Integer> headerIndex) {
    var out = new ArrayList<ImportRow>();
    for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
      var row = sheet.getRow(r);
      if (row == null) {
        continue;
      }
      var questionContent = requiredText(row, headerIndex, "question_content");
      if (questionContent == null) {
        continue;
      }
      out.add(
          new ImportRow(
              r + 1,
              requiredInt(row, headerIndex, "min_age"),
              requiredInt(row, headerIndex, "max_age"),
              questionContent,
              optionalText(row, headerIndex, "question_type", "MULTI"),
              requiredText(row, headerIndex, "option_content"),
              requiredInt(row, headerIndex, "suggest_flag"),
              optionalText(row, headerIndex, "improvement_tip", null),
              optionalInt(row, headerIndex, "sort_no", 0),
              requiredText(row, headerIndex, "dimension_code"),
              requiredText(row, headerIndex, "dimension_score")));
    }
    return out;
  }

  private static List<ImportRow> parseRowsChineseTemplate(
      org.apache.poi.ss.usermodel.Sheet sheet, Map<String, Integer> headerIndex) {
    var out = new ArrayList<ImportRow>();
    String lastQuestion = null;
    String lastQuestionType = null;
    Integer lastMinAge = null;
    Integer lastMaxAge = null;

    for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
      var row = sheet.getRow(r);
      if (row == null || isBlankRow(row, headerIndex)) {
        continue;
      }

      var question =
          optionalText(row, headerIndex, headerIndexOf(headerIndex, "问题"), null).orElse(null);
      if (question != null) {
        lastQuestion = question;
      } else if (lastQuestion == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "问题 is required");
      }

      var qtype =
          optionalText(
                  row,
                  headerIndex,
                  headerIndexOf(headerIndex, "问题类型（单选/多选）", "问题类型"),
                  null)
              .orElse(null);
      if (qtype != null) {
        lastQuestionType = qtype;
      }
      if (lastQuestionType == null) {
        lastQuestionType = "多选";
      }

      var minAgeText =
          optionalText(row, headerIndex, headerIndexOf(headerIndex, "适用最小年龄"), null).orElse(null);
      if (minAgeText != null) {
        lastMinAge = parseRequiredInt(minAgeText, "适用最小年龄");
      }
      if (lastMinAge == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "适用最小年龄 is required");
      }

      var maxAgeText =
          optionalText(row, headerIndex, headerIndexOf(headerIndex, "适用最大年龄"), null).orElse(null);
      if (maxAgeText != null) {
        lastMaxAge = parseRequiredInt(maxAgeText, "适用最大年龄");
      }
      if (lastMaxAge == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "适用最大年龄 is required");
      }

      var optionContent =
          requiredText(row, headerIndex, headerIndexOf(headerIndex, "选项值"), "选项值");
      var suggestFlagText =
          requiredText(row, headerIndex, headerIndexOf(headerIndex, "选项标识（建议/不建议）", "选项标识"), "选项标识");
      int suggestFlag = parseSuggestFlag(suggestFlagText);

      var improvementTip =
          optionalText(row, headerIndex, headerIndexOf(headerIndex, "改进文案"), null).orElse(null);

      var dim = parseDimensionScores(row, headerIndex);
      if (dim.dimensionCode.isBlank() || dim.dimensionScore.isBlank()) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "至少填写 1 个能力维度分值");
      }

      out.add(
          new ImportRow(
              r + 1,
              lastMinAge,
              lastMaxAge,
              lastQuestion,
              lastQuestionType,
              optionContent,
              suggestFlag,
              improvementTip,
              -1,
              dim.dimensionCode,
              dim.dimensionScore));
    }
    return out;
  }

  private static String requiredText(Row row, Map<String, Integer> headerIndex, String header) {
    var idx = headerIndex.get(header);
    if (idx == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "missing column: " + header);
    }
    var text = cellText(row.getCell(idx));
    if (text.isBlank()) {
      return null;
    }
    return text.trim();
  }

  private static String requiredText(
      Row row, Map<String, Integer> headerIndex, int idx, String fieldName) {
    var text = cellText(row.getCell(idx));
    if (text.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, fieldName + " is required");
    }
    return text.trim();
  }

  private static String optionalText(Row row, Map<String, Integer> headerIndex, String header, String fallback) {
    var idx = headerIndex.get(header);
    if (idx == null) {
      return fallback;
    }
    var text = cellText(row.getCell(idx));
    if (text.isBlank()) {
      return fallback;
    }
    return text.trim();
  }

  private static Optional<String> optionalText(
      Row row, Map<String, Integer> headerIndex, int idx, String fallback) {
    var text = cellText(row.getCell(idx));
    if (text.isBlank()) {
      return Optional.ofNullable(fallback);
    }
    return Optional.of(text.trim());
  }

  private static int requiredInt(Row row, Map<String, Integer> headerIndex, String header) {
    var idx = headerIndex.get(header);
    if (idx == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "missing column: " + header);
    }
    var value = cellText(row.getCell(idx));
    if (value.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, header + " is required");
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, header + " must be integer");
    }
  }

  private static int parseRequiredInt(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " is required");
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " must be integer");
    }
  }

  private static int optionalInt(Row row, Map<String, Integer> headerIndex, String header, int fallback) {
    var idx = headerIndex.get(header);
    if (idx == null) {
      return fallback;
    }
    var value = cellText(row.getCell(idx));
    if (value.isBlank()) {
      return fallback;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static Map<QuestionKey, List<ImportRow>> groupByQuestion(List<ImportRow> rows) {
    Map<QuestionKey, List<ImportRow>> grouped = new HashMap<>();
    for (var row : rows) {
      var key = new QuestionKey(row.minAge, row.maxAge, row.questionContent, normalizeQuestionType(row.questionType));
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
    }
    return grouped;
  }

  @Transactional
  void importOneQuestionGroup(List<ImportRow> group, boolean generateSortNo) {
    if (group.isEmpty()) {
      return;
    }
    var first = group.get(0);
    if (first.minAge < 0 || first.maxAge < 0 || first.minAge > 18 || first.maxAge > 18 || first.minAge > first.maxAge) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "invalid age range");
    }

    var questionType = normalizeQuestionType(first.questionType);
    if (questionRepo.existsSameQuestion(first.minAge, first.maxAge, questionType, first.questionContent)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "duplicate question already exists");
    }
    var questionId = questionRepo.insertQuestion(first.questionContent, first.minAge, first.maxAge, questionType, 1);

    int defaultSortNo = 1;
    for (var row : group) {
      var optionContent = row.optionContent.trim();
      if (optionContent.isBlank()) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "option_content is required");
      }
      var suggestFlag = row.suggestFlag == 0 ? 0 : 1;
      int sortNo = row.sortNo;
      if (generateSortNo || sortNo < 0) {
        sortNo = defaultSortNo++;
      }
      var optionId =
          questionRepo.insertOption(
              questionId,
              optionContent,
              suggestFlag,
              safeText(row.improvementTip),
              sortNo);

      var codes = splitCsv(row.dimensionCode);
      var scores = splitCsv(row.dimensionScore);
      if (codes.size() != scores.size()) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "dimension_code and dimension_score length mismatch");
      }
      var seen = new HashSet<String>();
      for (int i = 0; i < codes.size(); i++) {
        var code = codes.get(i).trim();
        var score = parsePositiveInt(scores.get(i), "dimension_score");
        var dim = CapabilityDimension.fromCode(code).orElse(null);
        if (dim == null) {
          throw new AppException(ErrorCode.INVALID_REQUEST, "dimension not found: " + code);
        }
        var normalized = dim.code().toUpperCase(Locale.ROOT);
        if (!seen.add(normalized)) {
          throw new AppException(ErrorCode.INVALID_REQUEST, "duplicate dimension: " + code);
        }
        questionRepo.insertOptionDimensionScore(optionId, normalized, score);
      }
    }
  }

  private static String normalizeQuestionType(String questionType) {
    var raw = questionType == null ? "" : questionType.trim();
    if ("单选".equals(raw) || "单选题".equals(raw)) {
      return "SINGLE";
    }
    if ("多选".equals(raw) || "多选题".equals(raw)) {
      return "MULTI";
    }
    var qt = raw.isEmpty() ? "MULTI" : raw.toUpperCase(Locale.ROOT);
    return ("SINGLE".equals(qt) || "MULTI".equals(qt)) ? qt : "MULTI";
  }

  private static int parseSuggestFlag(String raw) {
    if (raw == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "选项标识 is required");
    }
    var t = raw.trim();
    if (t.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "选项标识 is required");
    }
    if ("不建议".equals(t) || "否".equals(t) || "0".equals(t)) {
      return 0;
    }
    if ("建议".equals(t) || "正确".equals(t) || "是".equals(t) || "1".equals(t)) {
      return 1;
    }
    throw new AppException(ErrorCode.INVALID_REQUEST, "invalid 选项标识: " + t);
  }

  private static DimensionCsv parseDimensionScores(Row row, Map<String, Integer> headerIndex) {
    var codes = new ArrayList<String>();
    var scores = new ArrayList<String>();
    addDimensionIfPresent(row, headerIndex, "情绪管理力分值", "情绪管理力", CapabilityDimension.EMOTION_MANAGEMENT, codes, scores);
    addDimensionIfPresent(row, headerIndex, "沟通表达力分值", "沟通表达力", CapabilityDimension.COMMUNICATION_EXPRESSION, codes, scores);
    addDimensionIfPresent(row, headerIndex, "规则引导力分值", "规则引导力", CapabilityDimension.RULE_GUIDANCE, codes, scores);
    addDimensionIfPresent(row, headerIndex, "关系建设力分值", "关系建设力", CapabilityDimension.RELATIONSHIP_BUILDING, codes, scores);
    addDimensionIfPresent(row, headerIndex, "学习支持力分值", "学习支持力", CapabilityDimension.LEARNING_SUPPORT, codes, scores);
    return new DimensionCsv(String.join(",", codes), String.join(",", scores));
  }

  private static void addDimensionIfPresent(
      Row row,
      Map<String, Integer> headerIndex,
      String header1,
      String header2,
      CapabilityDimension dimension,
      List<String> codes,
      List<String> scores) {
    Integer idx = headerIndex.get(keyOf(header1));
    if (idx == null) {
      idx = headerIndex.get(keyOf(header2));
    }
    if (idx == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "missing column: " + header1);
    }
    var raw = cellText(row.getCell(idx));
    if (raw.isBlank()) {
      return;
    }
    var score = parsePositiveInt(raw, header1);
    codes.add(dimension.code());
    scores.add(Integer.toString(score));
  }

  private static int headerIndexOf(Map<String, Integer> headerIndex, String... aliases) {
    for (var alias : aliases) {
      var idx = headerIndex.get(keyOf(alias));
      if (idx != null) {
        return idx;
      }
    }
    throw new AppException(ErrorCode.INVALID_REQUEST, "missing column: " + aliases[0]);
  }

  private static boolean isBlankRow(Row row, Map<String, Integer> headerIndex) {
    for (var idx : headerIndex.values()) {
      var text = cellText(row.getCell(idx));
      if (!text.isBlank()) {
        return false;
      }
    }
    return true;
  }

  private static String keyOf(String header) {
    return header.trim().toLowerCase(Locale.ROOT);
  }

  private static int parsePositiveInt(String value, String field) {
    try {
      int v = Integer.parseInt(value.trim());
      if (v <= 0) {
        throw new AppException(ErrorCode.INVALID_REQUEST, field + " must be positive");
      }
      return v;
    } catch (NumberFormatException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + " must be integer");
    }
  }

  private static List<String> splitCsv(String value) {
    var out = new ArrayList<String>();
    if (value == null) {
      return out;
    }
    for (var part : value.split(",")) {
      var t = part.trim();
      if (!t.isBlank()) {
        out.add(t);
      }
    }
    return out;
  }

  private static String safeText(String text) {
    if (text == null) {
      return null;
    }
    var trimmed = text.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private static String cellText(Cell cell) {
    if (cell == null) {
      return "";
    }
    // 使用 DataFormatter 兼容 inlineStr / sharedStrings / numeric 等多种单元格类型；
    // 避免 setCellType 破坏原始单元格内容（且该 API 已废弃）。
    return DATA_FORMATTER.formatCellValue(cell);
  }

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();

  record ParseResult(List<ImportRow> rows, boolean generateSortNo) {}

  record DimensionCsv(String dimensionCode, String dimensionScore) {}

  record ImportRow(
      int rowNum,
      int minAge,
      int maxAge,
      String questionContent,
      String questionType,
      String optionContent,
      int suggestFlag,
      String improvementTip,
      int sortNo,
      String dimensionCode,
      String dimensionScore) {}

  record QuestionKey(int minAge, int maxAge, String questionContent, String questionType) {}
}
