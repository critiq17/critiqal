package org.critiqal.domain.auth.totp.service;

import org.critiqal.domain.auth.email.service.EmailService;
import org.critiqal.domain.auth.recovery.service.AccountRecoveryService;
import org.critiqal.domain.auth.totp.SecretEncryption;
import org.critiqal.domain.auth.totp.TotpProvider;
import org.critiqal.domain.auth.totp.TotpSecret;
import org.critiqal.domain.auth.totp.repository.TotpSecretRepository;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.User;
import org.critiqal.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TotpServiceImplTest {

    private final TotpSecretRepository totpRepo = mock(TotpSecretRepository.class);
    private final TotpProvider totpProvider = mock(TotpProvider.class);
    private final SecretEncryption secretEncryption = mock(SecretEncryption.class);
    private final UserService userService = mock(UserService.class);
    private final AccountRecoveryService recoveryService = mock(AccountRecoveryService.class);
    private final EmailService emailService = mock(EmailService.class);

    private TotpServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TotpServiceImpl(
                totpRepo,
                totpProvider,
                secretEncryption,
                userService,
                recoveryService,
                emailService
        );
    }

    @Test
    void disable_removesTotpAndRecoveryCodes_andTurnsOffFlag() {
        var user = user(1);
        user.twoFactorEnabled = true;

        var secret = new TotpSecret();
        secret.user = user;
        secret.secretEncrypted = "encrypted-secret";
        secret.confirmed = true;

        when(totpRepo.findByUserId(user.id)).thenReturn(Optional.of(secret));
        when(secretEncryption.decrypt("encrypted-secret")).thenReturn("raw-secret");
        when(totpProvider.verify("raw-secret", "123456")).thenReturn(true);
        when(userService.getById(user.id)).thenReturn(user);

        service.disable(user.id, "123456");

        verify(totpRepo).deleteByUserId(user.id);
        verify(recoveryService).deleteRecoveryCodes(user.id);
        verify(recoveryService, never()).regenerateRecoveryCodes(any());
        assertFalse(user.twoFactorEnabled);
    }

    @Test
    void verify_rejectsBlankCode() {
        var error = assertThrows(DomainException.class, () -> service.verify(uuid(5), " "));

        assertEquals("Invalid TOTP code", error.getMessage());
        verify(totpRepo, never()).findByUserId(any());
    }

    private static User user(long value) {
        var user = new User();
        user.id = uuid(value);
        user.username = "user_" + value;
        return user;
    }

    private static UUID uuid(long value) {
        return new UUID(0L, value);
    }
}
