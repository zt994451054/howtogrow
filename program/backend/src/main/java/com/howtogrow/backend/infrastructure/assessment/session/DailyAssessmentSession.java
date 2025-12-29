package com.howtogrow.backend.infrastructure.assessment.session;

import java.util.List;
import java.util.Set;

public record DailyAssessmentSession(
    long userId,
    long childId,
    List<Long> questionIdsByOrder,
    Set<Long> servedQuestionIds) {}

