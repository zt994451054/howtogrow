package com.howtogrow.backend.service.admin;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.controller.admin.dto.QuestionUpsertRequest;
import com.howtogrow.backend.domain.capability.CapabilityDimension;
import com.howtogrow.backend.infrastructure.admin.QuestionAdminRepository;
import java.util.HashSet;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminQuestionWriteService {
  private final QuestionAdminRepository questionRepo;

  public AdminQuestionWriteService(QuestionAdminRepository questionRepo) {
    this.questionRepo = questionRepo;
  }

  @Transactional
  public long create(QuestionUpsertRequest request) {
    validate(request);
    if (questionRepo.existsSameQuestion(request.minAge(), request.maxAge(), normalizeType(request.questionType()), request.content().trim())) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "题目已存在");
    }
    var questionId =
        questionRepo.insertQuestion(
            request.content().trim(),
            request.minAge(),
            request.maxAge(),
            normalizeType(request.questionType()),
            request.status());
    insertOptions(questionId, request);
    return questionId;
  }

  @Transactional
  public void update(long questionId, QuestionUpsertRequest request) {
    validate(request);
    questionRepo.updateQuestion(
        questionId,
        request.content().trim(),
        request.minAge(),
        request.maxAge(),
        normalizeType(request.questionType()),
        request.status());

    var optionIds = questionRepo.listOptionIdsByQuestion(questionId);
    questionRepo.deleteOptionDimensionScores(optionIds);
    questionRepo.softDeleteOptions(questionId);
    insertOptions(questionId, request);
  }

  @Transactional
  public void delete(long questionId) {
    var optionIds = questionRepo.listOptionIdsByQuestion(questionId);
    questionRepo.deleteOptionDimensionScores(optionIds);
    questionRepo.softDeleteOptions(questionId);
    questionRepo.softDeleteQuestion(questionId);
  }

  private void validate(QuestionUpsertRequest request) {
    if (request.minAge() == null || request.maxAge() == null) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "minAge/maxAge 不能为空");
    }
    if (request.minAge() < 0 || request.maxAge() < 0 || request.minAge() > 18 || request.maxAge() > 18) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "年龄范围不合法");
    }
    if (request.minAge() > request.maxAge()) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "minAge 必须小于等于 maxAge");
    }

    for (var opt : request.options()) {
      if (opt.dimensionScores() == null || opt.dimensionScores().isEmpty()) {
        throw new AppException(ErrorCode.INVALID_REQUEST, "dimensionScores 不能为空");
      }
      var seen = new HashSet<String>();
      for (var ds : opt.dimensionScores()) {
        var code = ds.dimensionCode() == null ? "" : ds.dimensionCode().trim();
        if (CapabilityDimension.fromCode(code).isEmpty()) {
          throw new AppException(ErrorCode.INVALID_REQUEST, "维度不存在：" + ds.dimensionCode());
        }
        var normalized = code.toUpperCase(Locale.ROOT);
        if (!seen.add(normalized)) {
          throw new AppException(ErrorCode.INVALID_REQUEST, "维度重复：" + ds.dimensionCode());
        }
      }
    }
  }

  private void insertOptions(long questionId, QuestionUpsertRequest request) {
    int sortNo = 1;
    for (var opt : request.options()) {
      var optionId =
          questionRepo.insertOption(
              questionId,
              opt.content().trim(),
              opt.suggestFlag(),
              safeText(opt.improvementTip()),
              sortNo++);
      for (var ds : opt.dimensionScores()) {
        var code = ds.dimensionCode() == null ? "" : ds.dimensionCode().trim();
        if (CapabilityDimension.fromCode(code).isEmpty()) {
          throw new AppException(ErrorCode.INVALID_REQUEST, "维度不存在：" + ds.dimensionCode());
        }
        questionRepo.insertOptionDimensionScore(optionId, code.toUpperCase(Locale.ROOT), ds.score());
      }
    }
  }

  private static String normalizeType(String type) {
    var t = type == null ? "MULTI" : type.trim().toUpperCase(Locale.ROOT);
    return ("SINGLE".equals(t) || "MULTI".equals(t)) ? t : "MULTI";
  }

  private static String safeText(String text) {
    if (text == null) {
      return null;
    }
    var trimmed = text.trim();
    return trimmed.isBlank() ? null : trimmed;
  }
}
