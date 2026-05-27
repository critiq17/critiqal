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
                "Your verification code — Critiqal",
                buildCodeEmail(
                        "Confirm your email",
                        "Use this code to finish setting up your Critiqal account. It expires in 15 minutes.",
                        code,
                        "Didn't try to sign up? You can safely ignore this email."
                )
        );
    }

    @Override
    public void sendLoginCode(String to, String code) {
        send(to,
                "Your sign-in code — Critiqal",
                buildCodeEmail(
                        "Sign in to Critiqal",
                        "Enter this code to finish signing in. It expires in 10 minutes.",
                        code,
                        "If this wasn't you, change your password right away — someone may have it."
                )
        );
    }

    @Override
    public void sendPasswordReset(String to, String resetUrl) {
        send(to,
                "Reset your password — Critiqal",
                buildLinkEmail(
                        "Reset your password",
                        "We got a request to reset your Critiqal password. Tap the button below to choose a new one. The link expires in 1 hour.",
                        resetUrl,
                        "Reset password",
                        "Didn't request a reset? You can ignore this — your account stays as it was."
                )
        );
    }

    @Override
    public void sendSecurityAlert(String to, String subject, String message) {
        send(to, subject, buildNoticeEmail("Security notice", message, null));
    }

    private void send(String to, String subject, String html) {
        try {
            mailer.send(Mail.withHtml(to, subject, html));
        } catch (Exception e) {
            log.errorf("Failed to send email to %s (subject: %s): %s", to, subject, e.getMessage());
            throw new RuntimeException("Email delivery failed", e);
        }
    }

    // ── HTML email templates ─────────────────────────────────────────────────
    // Light theme on purpose: Gmail/Outlook dark-mode strips or inverts dark
    // backgrounds inconsistently, leaving previous templates with invisible
    // grey-on-white codes. A bright card with a red accent reads cleanly
    // everywhere and matches the Critiqal star.
    //
    // Inline-only CSS, table-based layout — old mail clients ignore <style>.

    private static final String ACCENT   = "#e05252";
    private static final String INK      = "#0c0c0c";
    private static final String MUTED    = "#6b6b6b";
    private static final String SOFT_BG  = "#f5f4f1";
    private static final String LINE     = "#e7e5df";
    private static final String FONT     =
            "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif";

    private String buildCodeEmail(String title, String body, String code, String footer) {
        var codeBlock = """
                <table width="100%%" cellpadding="0" cellspacing="0" role="presentation" style="margin:8px 0 4px">
                  <tr>
                    <td align="center"
                        style="background:#fafaf8;border:1px solid %s;border-radius:12px;
                               padding:22px 16px">
                      <div style="font-family:ui-monospace,'SF Mono',Menlo,Consolas,monospace;
                                  font-size:34px;font-weight:700;color:%s;
                                  letter-spacing:0.32em;line-height:1">%s</div>
                    </td>
                  </tr>
                </table>
                """.formatted(LINE, INK, code);
        return shell(title, title, body, codeBlock, footer);
    }

    private String buildLinkEmail(String title, String body, String url, String btnText, String footer) {
        var button = """
                <table cellpadding="0" cellspacing="0" role="presentation" style="margin:8px 0 4px">
                  <tr>
                    <td align="center" bgcolor="%s"
                        style="background:%s;border-radius:10px">
                      <a href="%s"
                         style="display:inline-block;padding:13px 28px;font-family:%s;
                                font-size:14px;font-weight:600;letter-spacing:-0.005em;
                                color:#ffffff;text-decoration:none;border-radius:10px">%s</a>
                    </td>
                  </tr>
                </table>
                <p style="margin:18px 0 0;font-family:%s;font-size:12px;color:%s;line-height:1.55;
                          word-break:break-all">
                  Or paste this link into your browser:<br>
                  <span style="color:#9c9c9c">%s</span>
                </p>
                """.formatted(ACCENT, ACCENT, url, FONT, btnText, FONT, MUTED, url);
        return shell(title, title, body, button, footer);
    }

    private String buildNoticeEmail(String title, String body, String footer) {
        return shell(title, title, body, "", footer);
    }

    private String shell(String docTitle, String heading, String body, String mainBlock, String footer) {
        var footerHtml = (footer != null && !footer.isBlank()) ? """
                <p style="margin:24px 0 0;font-family:%s;font-size:12px;color:%s;line-height:1.6">%s</p>
                """.formatted(FONT, MUTED, footer) : "";

        // ZWJ-padded preheader keeps the inbox preview clean (Gmail/Apple Mail
        // otherwise leak the first body words after the visible preheader).
        var preheader = body;

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width,initial-scale=1" />
                  <meta name="color-scheme" content="light only" />
                  <meta name="supported-color-schemes" content="light" />
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:%s;font-family:%s;color:%s">
                  <div style="display:none;font-size:1px;color:%s;line-height:1px;max-height:0;max-width:0;opacity:0;overflow:hidden">
                    %s &#8199;&#65279;&#847; &#8199;&#65279;&#847; &#8199;&#65279;&#847; &#8199;&#65279;&#847;
                  </div>
                  <table width="100%%" cellpadding="0" cellspacing="0" role="presentation" style="background:%s">
                    <tr>
                      <td align="center" style="padding:40px 16px">
                        <table width="100%%" cellpadding="0" cellspacing="0" role="presentation"
                               style="max-width:480px">

                          <!-- Brand -->
                          <tr>
                            <td align="left" style="padding:0 4px 24px">
                              <table cellpadding="0" cellspacing="0" role="presentation">
                                <tr>
                                  <td style="vertical-align:middle;padding-right:8px;line-height:0">
                                    <span style="display:inline-block;color:%s;font-size:18px;line-height:1">&#9733;</span>
                                  </td>
                                  <td style="vertical-align:middle">
                                    <span style="font-family:%s;font-size:17px;font-weight:700;
                                                 color:%s;letter-spacing:-0.035em">critiqal</span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Card -->
                          <tr>
                            <td style="background:#ffffff;border:1px solid %s;border-radius:16px;
                                       padding:36px 32px;box-shadow:0 1px 2px rgba(15,15,15,0.04)">

                              <h1 style="margin:0 0 12px;font-family:%s;font-size:22px;font-weight:700;
                                         color:%s;letter-spacing:-0.02em;line-height:1.25">
                                %s
                              </h1>

                              <p style="margin:0 0 24px;font-family:%s;font-size:15px;color:%s;
                                        line-height:1.6">
                                %s
                              </p>

                              %s

                              %s

                            </td>
                          </tr>

                          <!-- Sign-off -->
                          <tr>
                            <td align="center" style="padding:24px 8px 0">
                              <p style="margin:0;font-family:%s;font-size:12px;color:%s;line-height:1.6">
                                Sent by Critiqal &middot; You're receiving this because of activity on your account.
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                        docTitle,                            // <title>
                        SOFT_BG, FONT, INK,                  // body bg + font + ink color
                        SOFT_BG, preheader,                  // preheader color + text
                        SOFT_BG,                             // outer table bg
                        ACCENT, FONT, INK,                   // brand star + wordmark
                        LINE,                                // card border
                        FONT, INK, heading,                  // h1
                        FONT, MUTED, body,                   // intro paragraph
                        mainBlock,                           // code block / button / nothing
                        footerHtml,                          // optional in-card footer
                        FONT, MUTED                          // sign-off
                );
    }
}
