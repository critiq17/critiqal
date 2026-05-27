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
    public void sendEmailVerificationCode(String to, String code) {
        send(to,
                "Your verification code - Critiqal",
                buildCodeEmail(
                        "Verify your email",
                        "Enter this code in the app. It expires in 15 minutes.",
                        code,
                        "If you didn't request this, ignore this message."
                )
        );
    }

    @Override
    public void sendLoginCode(String to, String code) {
        send(to,
                "Your sign-in code - Critiqal",
                buildCodeEmail(
                        "Sign in to Critiqal",
                        "Enter this code to finish signing in. It expires in 10 minutes.",
                        code,
                        "If this wasn't you, change your password immediately."
                )
        );
    }

    @Override
    public void sendPasswordReset(String to, String resetUrl) {
        send(to,
                "Reset your password — Critiqal",
                buildEmail(
                        "Reset your password",
                        "We received a request to reset your password. Click the button below to choose a new one. The link expires in 1 hour.",
                        resetUrl,
                        "Reset password",
                        "If you didn't request a password reset, your account is safe — no action needed."));
    }

    @Override
    public void sendSecurityAlert(String to, String subject, String message) {
        send(to, subject, buildEmail("Security notice", message, null, null, null));
    }

    private void send(String to, String subject, String html) {
        try {
            mailer.send(Mail.withHtml(to, subject, html));
        } catch (Exception e) {
            log.errorf("Failed to send email to %s (subject: %s): %s", to, subject, e.getMessage());
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    private String buildCodeEmail(String title, String body, String code, String footer) {
        return """
        <!DOCTYPE html>...
        <div style="font-size:48px;font-weight:800;letter-spacing:0.1em;
                    color:#eaeaea;text-align:center;padding:24px 0">
          %s
        </div>
        ...
        """.formatted(code);
    }

    private String buildEmail(String title, String body, String url, String btnText, String footer) {
        var btn = (url != null && btnText != null) ? """
                <a href="%s"
                   style="display:inline-block;background:#eaeaea;color:#0c0c0c;
                          text-decoration:none;padding:12px 28px;border-radius:8px;
                          font-size:14px;font-weight:600;letter-spacing:-0.01em;
                          margin-top:8px">
                  %s
                </a>
                """.formatted(url, btnText) : "";

        var fallback = (url != null) ? """
                <p style="margin:24px 0 0;font-size:12px;color:#555;word-break:break-all">
                  Or copy this link:<br>
                  <span style="color:#888">%s</span>
                </p>
                """.formatted(url) : "";

        var footerHtml = (footer != null) ? """
                <p style="margin:32px 0 0;font-size:12px;color:#555;line-height:1.6">%s</p>
                """.formatted(footer) : "";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width,initial-scale=1" />
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#0c0c0c;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',system-ui,sans-serif">
                  <table width="100%%" cellpadding="0" cellspacing="0" role="presentation">
                    <tr>
                      <td align="center" style="padding:48px 16px">
                        <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                               style="max-width:480px">

                          <!-- Logo -->
                          <tr>
                            <td style="padding-bottom:40px">
                              <span style="font-size:16px;font-weight:800;color:#eaeaea;
                                           letter-spacing:-0.045em">critiqal</span>
                            </td>
                          </tr>

                          <!-- Card -->
                          <tr>
                            <td style="background:#141414;border-radius:12px;padding:36px 32px">

                              <h1 style="margin:0 0 12px;font-size:20px;font-weight:700;
                                         color:#eaeaea;letter-spacing:-0.02em;line-height:1.3">
                                %s
                              </h1>

                              <p style="margin:0 0 28px;font-size:15px;color:#8c8c8c;
                                        line-height:1.6">
                                %s
                              </p>

                              %s

                              %s

                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding-top:28px">
                              %s
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(title, title, body, btn, fallback, footerHtml);
    }
}
