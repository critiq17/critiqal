package org.critiqal.domain.auth.email.service;

import java.util.UUID;

public interface EmailService {
    void sendEmailVerification(String to, String verificationCode);
    void sendPasswordReset(String to, String resetUrl);
    void sendSecurityAlert(String to, String subject, String message);
}
