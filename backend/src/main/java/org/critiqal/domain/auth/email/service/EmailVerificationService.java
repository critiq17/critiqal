package org.critiqal.domain.auth.email.service;

import java.util.UUID;

public interface EmailVerificationService {
    void sendEmailVerification(UUID userId, String email);
    void verify(String rawToken);
}
