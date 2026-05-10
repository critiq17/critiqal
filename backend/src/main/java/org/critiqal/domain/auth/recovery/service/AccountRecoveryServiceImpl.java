package org.critiqal.domain.auth.recovery.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@ApplicationScoped
public class AccountRecoveryServiceImpl implements AccountRecoveryService {

    private static final Logger log = Logger.getLogger(AccountRecoveryServiceImpl.class);

    private static final int CODE_COUNT = 8;
    private static final int CODE_SEGMENT_LEN = 4;
    private static final int CODE_SEGMENTS = 4;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final VerificationTokenRepository tokenRepo;
    private final RecoveryCodeRepository recoveryCodeRepo;
    private final UserRepository userRepo;
    private final UserService userService;
    private final EmailService emailService;
    private final String appBaseUrl;
    private final int resetExpiryHours;
    private final SecureRandom random = new SecureRandom();

    public AccountRecoveryServiceImpl(
            VerificationTokenRepository tokenRepo,
            RecoveryCodeRepository recoveryCodeRepo,
            UserRepository userRepo,
            UserService userService,
            EmailService emailService,
            @ConfigProperty(name = "app.base-url") String appBaseUrl,
            @ConfigProperty(name = "auth.token-password-reset-expiry-hours", defaultValue = "1") int resetExpiryHours) {
        this.tokenRepo = tokenRepo;
        this.recoveryCodeRepo = recoveryCodeRepo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.emailService = emailService;
        this.appBaseUrl = appBaseUrl;
        this.resetExpiryHours = resetExpiryHours;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        if (email == null || email.isBlank()) return;

        var maybeUser = userRepo.findByEmail(email.trim().toLowerCase());
        if (maybeUser.isEmpty() || !maybeUser.get().emailVerified) {
            simulateTokenWork();
            return;
        }

        var user = maybeUser.get();
        tokenRepo.deleteByUserIdAndType(user.id, VerificationTokenType.PASSWORD_RESET);

        var rawToken = generateSecureToken();
        var token = new VerificationToken();
        token.user = user;
        token.tokenHash = hashToken(rawToken);
        token.type = VerificationTokenType.PASSWORD_RESET;
        token.expiresAt = Instant.now().plus(resetExpiryHours, ChronoUnit.HOURS);
        tokenRepo.save(token);

        var url = appBaseUrl + "/reset-password?token=" + rawToken;
        emailService.sendPasswordReset(user.email, url);
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        validatePasswordStrength(newPassword);
        requireResetToken(rawToken);

        var hash = hashToken(rawToken);
        var token = tokenRepo.findByTokenHashAndType(hash, VerificationTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new DomainException("Invalid or expired reset link"));

        if (!token.isValid()) {
            throw new DomainException("Reset link has expired or already been used");
        }

        token.usedAt = Instant.now();
        token.user.passwordHash = BcryptUtil.bcryptHash(newPassword);

        log.infof("Password reset for userId=%s", token.user.id);
    }

    @Override
    @Transactional
    public UUID useRecoveryCode(String username, String rawCode) {
        var normalizedUsername = parseRecoveryUsername(username);
        var normalizedCode = normalizeCode(rawCode);
        if (normalizedCode == null) {
            throw new DomainException("Invalid recovery code");
        }

        var user = userRepo.findByUsername(normalizedUsername)
                .filter(existing -> existing.twoFactorEnabled)
                .orElseThrow(() -> new DomainException("Invalid recovery code"));

        var activeCodes = recoveryCodeRepo.findActiveByUserId(user.id);

        for (var code : activeCodes) {
            if (BcryptUtil.matches(normalizedCode, code.codeHash)) {
                code.usedAt = Instant.now();
                log.infof("Recovery code used for userId=%s, codeId=%s", user.id, code.id);
                return user.id;
            }
        }
        throw new DomainException("Invalid recovery code");
    }


    @Override
    @Transactional
    public List<String> regenerateRecoveryCodes(UUID userId) {
        var user = userService.getById(userId);

        recoveryCodeRepo.deleteByUserId(userId);

        var rawCodes = IntStream.range(0, CODE_COUNT)
                .mapToObj(i -> generateCode())
                .toList();

        var entities = rawCodes.stream().map(raw -> {
            var code = new RecoveryCode();
            code.user = user;
            code.codeHash = BcryptUtil.bcryptHash(normalizeCode(raw));
            return code;
        }).toList();

        recoveryCodeRepo.saveAll(entities);

        if (user.email != null && user.emailVerified) {
            emailService.sendSecurityAlert(
                    user.email,
                    "Recovery codes regenerated",
                    "Your Critiqal recovery codes have been regenerated. " +
                            "If you did not request this, please contact support immediately."
            );
        }
        return rawCodes;
    }

    @Override
    @Transactional
    public void deleteRecoveryCodes(UUID userId) {
        recoveryCodeRepo.deleteByUserId(userId);
    }

    @Override
    public long countActiveRecoveryCodes(UUID userId) {
        return recoveryCodeRepo.countActiveByUserId(userId);
    }

    private String generateCode() {
        var sb = new StringBuilder();
        for (int segment = 0; segment < CODE_SEGMENTS; segment++) {
            if (segment > 0) sb.append('-');
            for (int i = 0; i < CODE_SEGMENT_LEN; i++) {
                sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
            }
        }
        return sb.toString();
    }

    private String generateSecureToken() {
        var bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(raw.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Hash failed", e);
        }
    }

    private Username parseRecoveryUsername(String username) {
        try {
            return Username.of(username);
        } catch (DomainException e) {
            throw new DomainException("Invalid recovery code");
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.replace("-", "").replace(" ", "").toUpperCase();
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new DomainException("Password must be at least 8 characters");
        }
    }

    private void requireResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new DomainException("Invalid or expired reset link");
        }
    }

    private void simulateTokenWork() {
        BcryptUtil.bcryptHash("dummy-prevent-timing-attack");
    }
}
