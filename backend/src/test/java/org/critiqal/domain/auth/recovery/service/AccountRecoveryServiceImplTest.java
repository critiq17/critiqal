package org.critiqal.domain.auth.recovery.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import org.critiqal.domain.auth.email.service.EmailService;
import org.critiqal.domain.auth.recovery.RecoveryCode;
import org.critiqal.domain.auth.recovery.repository.RecoveryCodeRepository;
import org.critiqal.domain.auth.verification.VerificationToken;
import org.critiqal.domain.auth.verification.VerificationTokenType;
import org.critiqal.domain.auth.verification.repository.VerificationTokenRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.Username;
import org.critiqal.domain.user.repository.UserRepository;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountRecoveryServiceImplTest {

    private final VerificationTokenRepository tokenRepo = mock(VerificationTokenRepository.class);
    private final RecoveryCodeRepository  codeRepo = mock(RecoveryCodeRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);
    private final EmailService emailService = mock(EmailService.class);

    private AccountRecoveryServiceImpl service;

    @BeforeEach void setUp() {
        service = new AccountRecoveryServiceImpl(tokenRepo, codeRepo, userRepo,
                userService, emailService, "https://critiqal.xyz", 1);
    }

    @Test
    void requestReset_silentForUnknownEmail() {
        when(userRepo.findByEmail("ghost@x.com")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.requestPasswordReset("ghost@x.com"));
        verify(emailService, never()).sendPasswordReset(any(), any());
    }

    @Test
    void requestReset_silentForUnverifiedEmail() {
        var user = user(1); user.email = "u@x.com"; user.emailVerified = false;
        when(userRepo.findByEmail("u@x.com")).thenReturn(Optional.of(user));
        service.requestPasswordReset("u@x.com");
        verify(emailService, never()).sendPasswordReset(any(), any());
    }

    @Test
    void requestReset_sendsEmail_forVerifiedEmail() {
        var user = user(1); user.email = "v@x.com"; user.emailVerified = true;
        when(userRepo.findByEmail("v@x.com")).thenReturn(Optional.of(user));
        service.requestPasswordReset("v@x.com");
        verify(tokenRepo).save(any(VerificationToken.class));
        verify(emailService).sendPasswordReset(eq("v@x.com"), any());
    }

    @Test
    void resetPassword_throwsForExpiredToken() {
        var token = new VerificationToken();
        token.expiresAt = Instant.now().minus(1, ChronoUnit.HOURS);
        when(tokenRepo.findByTokenHashAndType(any(), eq(VerificationTokenType.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));
        assertThrows(DomainException.class, () -> service.resetPassword("any-token", "newpass123"));
    }

    @Test
    void resetPassword_updatesHash_andMarksUsed() {
        var user = user(1); user.passwordHash = "old";
        var token = new VerificationToken();
        token.user = user; token.expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        when(tokenRepo.findByTokenHashAndType(any(), eq(VerificationTokenType.PASSWORD_RESET)))
                .thenReturn(Optional.of(token));

        service.resetPassword("valid", "newpassword123");

        assertNotNull(token.usedAt);
        assertTrue(BcryptUtil.matches("newpassword123", user.passwordHash));
    }

    @Test
    void useRecoveryCode_marksCodeUsed_andReturnsUserId() {
        var user = user(7);
        user.twoFactorEnabled = true;
        when(userRepo.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));
        var code = new RecoveryCode();
        code.user = user;
        code.codeHash = BcryptUtil.bcryptHash("ABCDEFGHIJKLMNOP");
        when(codeRepo.findActiveByUserId(user.id)).thenReturn(List.of(code));

        var result = service.useRecoveryCode("alice", "ABCD-EFGH-IJKL-MNOP");

        assertEquals(user.id, result);
        assertNotNull(code.usedAt);
    }

    @Test
    void useRecoveryCode_rejectsDisabledTwoFactorUser() {
        var user = user(8);
        user.twoFactorEnabled = false;
        when(userRepo.findByUsername(Username.of("alice"))).thenReturn(Optional.of(user));

        var error = assertThrows(DomainException.class,
                () -> service.useRecoveryCode("alice", "ABCD-EFGH-IJKL-MNOP"));

        assertEquals("Invalid recovery code", error.getMessage());
        verify(codeRepo, never()).findActiveByUserId(any());
    }

    @Test
    void useRecoveryCode_rejectsMalformedUsernameWithoutLeakingValidationDetails() {
        var error = assertThrows(DomainException.class,
                () -> service.useRecoveryCode("x", "ABCD-EFGH-IJKL-MNOP"));

        assertEquals("Invalid recovery code", error.getMessage());
        verifyNoInteractions(codeRepo);
    }

    @Test
    void resetPassword_rejectsBlankToken() {
        var error = assertThrows(DomainException.class,
                () -> service.resetPassword(" ", "newpassword123"));

        assertEquals("Invalid or expired reset link", error.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    void regenerateCodes_returns8FormattedCodes() {
        var user = user(3);
        when(userService.getById(user.id)).thenReturn(user);

        var codes = service.regenerateRecoveryCodes(user.id);

        assertThat(codes).hasSize(8);
        codes.forEach(c -> assertThat(c).matches("[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}-[A-Z2-9]{4}"));
        verify(codeRepo).deleteByUserId(user.id);
        var cap = ArgumentCaptor.forClass(List.class);
        verify(codeRepo).saveAll(cap.capture());
        assertThat(cap.getValue()).hasSize(8);
    }

    @Test
    void deleteRecoveryCodes_delegatesToRepository() {
        service.deleteRecoveryCodes(user(10).id);

        verify(codeRepo).deleteByUserId(new UUID(0, 10));
    }

    private static User user(int n) {
        var u = new User(); u.id = new UUID(0, n); return u;
    }
}
