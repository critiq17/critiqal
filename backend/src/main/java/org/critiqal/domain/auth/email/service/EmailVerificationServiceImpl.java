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
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
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
    private final String appBaseUrl;
    private final int expiryHours;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationServiceImpl(
            VerificationTokenRepository repo,
            UserRepository userRepo,
            UserService userService,
            EmailService emailService,
            @ConfigProperty(name = "app.base-url") String appBaseUrl,
            @ConfigProperty(name = "auth.token.email-verify-expiry-hours", defaultValue = "24") int expiryHours) {
        this.tokenRepo = repo;
        this.userRepo = userRepo;
        this.userService = userService;
        this.emailService = emailService;
        this.appBaseUrl = appBaseUrl;
        this.expiryHours = expiryHours;
    }

    @Override
    public void sendEmailVerification(UUID userId, String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new DomainException("Invalid email address");
        }

        var normalizedEmail = email.trim().toLowerCase();

        // Persist the pending email + token in its own committed transaction.
        // Mail delivery is external I/O and must not run inside a DB transaction
        // — a delivery failure should not discard the saved pending state.
        var rawToken = QuarkusTransaction.requiringNew()
                .call(() -> persistPendingEmail(userId, normalizedEmail));

        var url = appBaseUrl + "/verify-email?token=" + rawToken;
        emailService.sendEmailVerification(normalizedEmail, url);
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

        var rawToken = generateSecureToken();
        var token = new VerificationToken();
        token.user = user;
        token.tokenHash = hashToken(rawToken);
        token.type = VerificationTokenType.EMAIL_VERIFY;
        token.email = normalizedEmail;
        token.expiresAt = Instant.now().plus(expiryHours, ChronoUnit.HOURS);
        tokenRepo.save(token);

        return rawToken;
    }

    @Override
    @Transactional
    public void verify(String rawToken) {
        var hash = hashToken(rawToken);
        var token = tokenRepo.findByTokenHashAndType(hash, VerificationTokenType.EMAIL_VERIFY)
                .orElseThrow(() -> new DomainException("Invalid verification link"));

        if (!token.isValid()) {
            throw new DomainException("Verification link has expired or already been used");
        }

        token.usedAt = Instant.now();

        var user = token.user;
        user.email = token.email;
        user.emailVerified = true;
        user.pendingEmail = null;
    }

    static String generateSecureToken() {
        var bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
