package com.howtogrow.backend.infrastructure.assessment.session;

import java.util.Optional;

public interface DailyAssessmentSessionStore {
  void save(DailyAssessmentSession session, String sessionId);

  Optional<DailyAssessmentSession> find(long userId, long childId, String sessionId);

  void delete(long userId, long childId, String sessionId);
}

