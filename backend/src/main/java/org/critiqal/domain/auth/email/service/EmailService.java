package org.critiqal.domain.auth.email.service;

import java.util.UUID;

public interface EmailService {
    void sendEmailVerificationCode(String to, String code);
    void sendPasswordReset(String to, String resetUrl);
    void sendSecurityAlert(String to, String subject, String message);
}
