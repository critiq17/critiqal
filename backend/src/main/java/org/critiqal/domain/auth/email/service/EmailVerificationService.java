package org.critiqal.domain.auth.email.service;

import java.util.UUID;

public interface EmailVerificationService {
    void assertEmailAvailable(String email);
    void sendEmailVerification(UUID userId, String email);
    void verify(UUID userId, String rawCode);
    void resendVerification(UUID userId);
}
