package com.howtogrow.backend.service.miniprogram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.howtogrow.backend.api.ErrorCode;
import com.howtogrow.backend.api.exception.AppException;
import com.howtogrow.backend.domain.time.BizClock;
import com.howtogrow.backend.infrastructure.user.UserAccountRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MiniprogramMeServiceTest {
  @Test
  void getMe_userMissing_throwsUnauthorized() {
    var clock = mock(BizClock.class);
    var repo = mock(UserAccountRepository.class);
    when(repo.findById(123L)).thenReturn(Optional.empty());

    var service = new MiniprogramMeService(clock, repo);
    var ex = assertThrows(AppException.class, () -> service.getMe(123L));
    assertEquals(ErrorCode.UNAUTHORIZED, ex.code());
  }

  @Test
  void updateProfile_userMissing_throwsUnauthorized() {
    var clock = mock(BizClock.class);
    when(clock.today()).thenReturn(LocalDate.of(2026, 1, 18));
    var repo = mock(UserAccountRepository.class);
    when(repo.findById(123L)).thenReturn(Optional.empty());

    var service = new MiniprogramMeService(clock, repo);
    var ex = assertThrows(AppException.class, () -> service.updateProfile(123L, "昵称", "a", null));
    assertEquals(ErrorCode.UNAUTHORIZED, ex.code());
  }
}

