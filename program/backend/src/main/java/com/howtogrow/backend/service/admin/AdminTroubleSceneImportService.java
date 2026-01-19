package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.TroubleSceneImportResponse;
import com.howtogrow.backend.infrastructure.trouble.TroubleSceneRepository;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AdminTroubleSceneImportService {
  private final TroubleSceneRepository sceneRepo;

  public AdminTroubleSceneImportService(TroubleSceneRepository sceneRepo) {
    this.sceneRepo = sceneRepo;
  }

  @Transactional
  public TroubleSceneImportResponse importExcel(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "请上传文件");
    }
    try (InputStream in = file.getInputStream(); var workbook = WorkbookFactory.create(in)) {
      var sheet = workbook.getSheetAt(0);
      if (sheet == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "Excel 为空");
      }
      var headerRow = sheet.getRow(sheet.getFirstRowNum());
      if (headerRow == null) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "缺少表头行");
      }

      var headerIndex = parseHeaderIndex(headerRow);
      int nameIdx = headerIndexOf(headerIndex, "name", "名称");
      Integer logoIdx = optionalHeaderIndexOf(headerIndex, "logo_url", "logo", "logo图片url", "logo图片", "logo图片url（可选）");
      int minAgeIdx = headerIndexOf(headerIndex, "min_age", "最小年龄", "适用最小年龄");
      int maxAgeIdx = headerIndexOf(headerIndex, "max_age", "最大年龄", "适用最大年龄");

      var seenNames = new HashSet<String>();
      int imported = 0;
      for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
        var row = sheet.getRow(r);
        if (row == null) continue;

        var name = cellText(row.getCell(nameIdx)).trim();
        if (name.isBlank()) continue;
        if (!seenNames.add(name)) {
          throw new AppException(ErrorCode.INVALID_REQUEST, "导入文件内存在重复名称：" + name);
        }

        var logoUrl = logoIdx == null ? null : safeText(cellText(row.getCell(logoIdx)));
        int minAge = parseRequiredInt(cellText(row.getCell(minAgeIdx)), "minAge");
        int maxAge = parseRequiredInt(cellText(row.getCell(maxAgeIdx)), "maxAge");
        validateAgeRange(minAge, maxAge);

        try {
          sceneRepo.create(name, logoUrl, minAge, maxAge);
          imported++;
        } catch (DuplicateKeyException e) {
          throw new AppException(ErrorCode.INVALID_REQUEST, "名称已存在：" + name);
        }
      }

      if (imported == 0) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "未找到可导入的数据");
      }
      return new TroubleSceneImportResponse(imported);
    } catch (AppException e) {
      throw e;
    } catch (Exception e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Excel 文件不合法");
    }
  }

  private static void validateAgeRange(int minAge, int maxAge) {
    if (minAge < 0 || maxAge < 0 || minAge > 18 || maxAge > 18 || minAge > maxAge) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "年龄范围不合法");
    }
  }

  private static int parseRequiredInt(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + "不能为空");
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new AppException(ErrorCode.INVALID_REQUEST, field + "必须为整数");
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

  private static int headerIndexOf(Map<String, Integer> headerIndex, String... aliases) {
    for (var alias : aliases) {
      var idx = headerIndex.get(keyOf(alias));
      if (idx != null) {
        return idx;
      }
    }
    throw new AppException(ErrorCode.INVALID_REQUEST, "缺少列：" + aliases[0]);
  }

  private static Integer optionalHeaderIndexOf(Map<String, Integer> headerIndex, String... aliases) {
    for (var alias : aliases) {
      var idx = headerIndex.get(keyOf(alias));
      if (idx != null) {
        return idx;
      }
    }
    return null;
  }

  private static String keyOf(String header) {
    return header.trim().toLowerCase(Locale.ROOT);
  }

  private static String safeText(String text) {
    if (text == null) return null;
    var trimmed = text.trim();
    return trimmed.isBlank() ? null : trimmed;
  }

  private static String cellText(Cell cell) {
    if (cell == null) {
      return "";
    }
    return DATA_FORMATTER.formatCellValue(cell);
  }

  private static final DataFormatter DATA_FORMATTER = new DataFormatter();
}

