package org.critiqal.domain.auth.email.service;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QuarkusMailerEmailService implements EmailService {

    private static final Logger log = Logger.getLogger(QuarkusMailerEmailService.class);

    private final Mailer mailer;

    public QuarkusMailerEmailService(Mailer mailer) {
        this.mailer = mailer;
    }

    @Override
    public void sendEmailVerification(String to, String verificationUrl) {
        send(to,
                "Verify your Critiqal email",
                emailHtml(
                        "Verify your email address",
                        "Click below to verify your email. This link expires in 24 hours.",
                        verificationUrl, "Verify Email"));
    }

    @Override
    public void sendPasswordReset(String to, String resetUrl) {
        send(to,
                "Reset your Critiqal password",
                emailHtml(
                        "Reset your password",
                        "Click below to reset your password. This link expires in 1 hour.",
                        resetUrl, "Reset Password"));
    }

    @Override
    public void sendSecurityAlert(String to, String subject, String message) {
        send(to, subject,
                "<html><body style=\"font-family:sans-serif;max-width:600px;margin:0 auto\">" +
                        "<p>" + message + "</p></body></html>");;
    }

    private void send(String to, String subject, String html) {
        try {
            mailer.send(Mail.withHtml(to, subject, html));
        } catch (Exception e) {
            log.errorf("Failed to send email to %s (subject: %s): %s", to, subject, e.getMessage());
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String emailHtml(String title, String body, String url, String btnText) {
        return """
               <html><body style="font-family:sans-serif;max-width:600px;margin:0 auto;padding:32px">
               <h2>%s</h2>
               <p>%s</p>
               <p>
                 <a href="%s"
                    style="background:#000;color:#fff;padding:12px 24px;
                           text-decoration:none;border-radius:4px;display:inline-block">
                   %s
                 </a>
               </p>
               <p style="color:#666;font-size:12px">
                 Or copy this link: <br><code>%s</code>
               </p>
               <p style="color:#999;font-size:11px">
                 If you didn't request this, you can safely ignore this email.
               </p>
               </body></html>
               """.formatted(title, body, url, btnText, url);
    }
}
