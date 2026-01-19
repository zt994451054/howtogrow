package com.howtogrow.backend.infrastructure.child;

import java.time.LocalDate;

public record Child(long id, long userId, String nickname, int gender, LocalDate birthDate, String parentIdentity) {}
