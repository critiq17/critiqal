package org.critiqal.domain.auth.email.service;

import java.util.UUID;

public interface EmailVerificationService {
    void assertEmailAvailable(String email);
    void sendEmailVerification(UUID userId, String email);
    void verify(UUID userId, String rawCode);
    void resendVerification(UUID userId);

    /** Email-based second factor: send a fresh code to the user's verified address. */
    void sendLoginCode(UUID userId);

    /** Validate the login code; throws DomainException on invalid/expired. */
    void verifyLoginCode(UUID userId, String rawCode);
}
