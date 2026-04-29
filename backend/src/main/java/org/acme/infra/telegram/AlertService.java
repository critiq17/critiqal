package org.acme.infra.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AlertService {

    private static final Logger log = Logger.getLogger(AlertService.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    @Inject TelegramClient telegramClient;

    /**
     *
     * @param level
     * @param title
     * @param details
     */
    public void send(AlertLevel level, String title, String details) {
        var time = ZonedDateTime.now(ZoneId.of("UTC")).format(FMT);
        var sb = new StringBuilder();

        sb.append(level.level).append(" <b>").append(escape(title)).append("</b>\n");

        if (details != null && !details.isBlank()) {
            sb.append(escape(details)).append("\n");
        }

        sb.append("\n<i>").append(time).append(" UTC</i>");

        log.debugf("Alert [%s] %s", level, title);
        telegramClient.sendMessage(sb.toString());
    }

    public void info(String title, String details) {
        send(AlertLevel.INFO, title, details);
    }

    public void warn(String title, String details) {
        send(AlertLevel.WARN, title, details);
    }

    public void error(String title, String details) {
        send(AlertLevel.ERROR, title, details);
    }

    public void error(String title, Throwable ex) {
        var details = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        send(AlertLevel.ERROR, title, details);
    }

    private String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
