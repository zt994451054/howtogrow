package com.howtogrow.backend.infrastructure.assessment;

import java.time.Instant;

public record DailyAssessment(long id, long userId, long childId, Instant submittedAt) {}
