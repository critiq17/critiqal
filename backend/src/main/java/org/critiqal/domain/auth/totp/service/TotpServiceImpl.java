package org.critiqal.domain.auth.totp.service;

import io.smallrye.config.inject.ConfigException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.auth.email.service.EmailService;
import org.critiqal.domain.auth.recovery.service.AccountRecoveryService;
import org.critiqal.domain.auth.totp.SecretEncryption;
import org.critiqal.domain.auth.totp.TotpProvider;
import org.critiqal.domain.auth.totp.TotpSecret;
import org.critiqal.domain.auth.totp.TotpSetupResult;
import org.critiqal.domain.auth.totp.repository.TotpSecretRepository;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.service.UserService;

import java.util.UUID;

@ApplicationScoped
public class TotpServiceImpl implements TotpService {

    private final TotpSecretRepository totpRepo;
    private final TotpProvider totpProvider;
    private final SecretEncryption  secretEncryption;
    private final UserService userService;
    private final AccountRecoveryService recoveryService;
    private final EmailService emailService;

    public TotpServiceImpl(
            TotpSecretRepository totpRepo,
            TotpProvider totpProvider,
            SecretEncryption secretEncryption,
            UserService userService,
            AccountRecoveryService accountRecoveryService,
            EmailService emailService) {
        this.totpRepo = totpRepo;
        this.totpProvider = totpProvider;
        this.secretEncryption = secretEncryption;
        this.userService = userService;
        this.recoveryService = accountRecoveryService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public TotpSetupResult setup(UUID userId) {
        var user = userService.getById(userId);
        if (user.twoFactorEnabled) {
            throw new ConflictException("Two-factor authentication is already enabled");
        }
        
        totpRepo.deleteByUserId(userId);

        var rawSecret = totpProvider.generateSecret();
        var encryptedSecret = secretEncryption.encrypt(rawSecret);

        var totpSecret = new TotpSecret();
        totpSecret.user = user;
        totpSecret.secretEncrypted = encryptedSecret;
        totpSecret.confirmed = false;
        totpRepo.save(totpSecret);

        var qrUri = totpProvider.generateQrCodeUri(rawSecret, user.username);
        var recoveryCodes = recoveryService.regenerateRecoveryCodes(userId);

        return new TotpSetupResult(qrUri, rawSecret, recoveryCodes);
    }

    @Override
    @Transactional
    public void confirm(UUID userId, String totpCode) {
        var secret = totpRepo.findByUserId(userId)
                .orElseThrow(() -> new DomainException("2FA setup not initiated"));
        if (secret.confirmed) throw new ConflictException("2FA is already active");

        var rawSecret = secretEncryption.decrypt(secret.secretEncrypted);
        if (!totpProvider.verify(rawSecret, totpCode)) {
            throw new DomainException("Invalid TOTP code");
        }

        secret.confirmed = true;
        var user = userService.getById(userId);
        user.twoFactorEnabled = true;

        if (user.email != null && user.emailVerified) {
            emailService.sendSecurityAlert(user.email,
                    "Two-factor authentication enabled",
                    "2FA has been enabled on your Critiqal account. " +
                    "If you did not do this, contact support immediately.");
        }
    }

    @Override
    public void verify(UUID userId, String totpCode) {
        var secret = totpRepo.findByUserId(userId)
                .filter(s -> s.confirmed)
                .orElseThrow(() -> new DomainException("2FA is not configured"));

    var rawSecret = secretEncryption.decrypt(secret.secretEncrypted);
    if (!totpProvider.verify(rawSecret, totpCode)) {
        throw new DomainException("Invalid TOTP code");
        }
    }

    @Override
    @Transactional
    public void disable(UUID userId, String totpCode) {
        verify(userId, totpCode); // check rules

        totpRepo.deleteByUserId(userId);
        recoveryService.regenerateRecoveryCodes(userId);
        var user = userService.getById(userId);
        user.twoFactorEnabled = false;

        if (user.email != null && user.emailVerified) {
            emailService.sendSecurityAlert(user.email,
                    "Two-factor authentication disabled",
                    "2FA has been disabled on your account. " +
                    "If you did not do this, contact support immediately.");
        }
    }

    @Override
    public boolean isEnabled(UUID userId) {
        return userService.getById(userId).twoFactorEnabled;
    }
}
