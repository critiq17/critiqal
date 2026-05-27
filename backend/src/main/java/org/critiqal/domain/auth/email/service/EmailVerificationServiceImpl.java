package org.critiqal.domain.auth.email.service;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.auth.verification.VerificationToken;
import org.critiqal.domain.auth.verification.VerificationTokenType;
import org.critiqal.domain.auth.verification.repository.VerificationTokenRepository;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.repository.UserRepository;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.auth.RateLimiter;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

@ApplicationScoped
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final VerificationTokenRepository tokenRepo;
    private final UserRepository userRepo;
    private final UserService userService;
    private final EmailService emailService;
    private final RateLimiter rateLimiter;
    private final String appBaseUrl;
    private final int expiryMinutes;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationServiceImpl(
            VerificationTokenRepository repo,
            UserRepository userRepo,
            UserService userService,
            EmailService emailService,
            RateLimiter rateLimiter,
            @ConfigProperty(name = "app.base-url") String appBaseUrl,
            @ConfigProperty(name = "auth.token.email-verify-expiry-minutes", defaultValue = "15") int expiryMinutes) {
        this.tokenRepo = repo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.emailService = emailService;
        this.rateLimiter = rateLimiter;
        this.appBaseUrl = appBaseUrl;
        this.expiryMinutes = expiryMinutes;
    }

    @Override
    public void sendEmailVerification(UUID userId, String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new DomainException("Invalid email address");
        }

        var normalizedEmail = email.trim().toLowerCase();

        rateLimiter.check(RateLimiter.key("email-verify", normalizedEmail), 5, Duration.ofHours(1));
        // Persist the pending email + token in its own committed transaction.
        // Mail delivery is external I/O and must not run inside a DB transaction
        // — a delivery failure should not discard the saved pending state.
        var rawCode = QuarkusTransaction.requiringNew()
                .call(() -> persistPendingEmail(userId, normalizedEmail));

        emailService.sendEmailVerificationCode(normalizedEmail, rawCode);
    }

    private String persistPendingEmail(UUID userId, String normalizedEmail) {
        userRepo.findByEmail(normalizedEmail).ifPresent(existing -> {
            if (!existing.id.equals(userId)) {
                throw new ConflictException("Email is already in use");
            }
        });

        tokenRepo.deleteByUserIdAndType(userId, VerificationTokenType.EMAIL_VERIFY);

        var user = userService.getById(userId);
        user.pendingEmail = normalizedEmail;

        var rawToken = generateVerificationCode();
        var token = new VerificationToken();
        token.user = user;
        token.tokenHash = hashToken(rawToken);
        token.type = VerificationTokenType.EMAIL_VERIFY;
        token.email = normalizedEmail;
        token.expiresAt = Instant.now().plus(expiryMinutes, ChronoUnit.MINUTES);
        tokenRepo.save(token);

        return rawToken;
    }

    @Override
    @Transactional
    public void verify(UUID userId, String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new DomainException("Verification code is required");
        }

        var hash = hashToken(rawCode.strip());
        var token = tokenRepo.findByTokenHashAndType(hash, VerificationTokenType.EMAIL_VERIFY)
                .orElseThrow(() -> new DomainException("Invalid verification code"));

        if (!token.user.id.equals(userId)) {
            throw new DomainException("Invalid verification code");
        }

        if (!token.isValid()) {
            throw new DomainException("Verification code has expired");
        }

        token.usedAt = Instant.now();
        var user = token.user;
        user.email = token.email;
        user.emailVerified = true;
        user.pendingEmail = null;
    }

    @Override
    public void resendVerification(UUID userId) {
        var user = userService.getById(userId);
        if (user.emailVerified) {
            throw new ConflictException("Email is already verified");
        }

        if (user.pendingEmail == null) {
            throw new DomainException("No pending email to verify");
        }
        sendEmailVerification(userId, user.pendingEmail);
    }

    private String generateVerificationCode() {
        int code = random.nextInt(900_000) + 100_000;
        return String.valueOf(code);
    }

    static String hashToken(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(digest.digest(raw.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Hash failed", e);
        }
    }
}
