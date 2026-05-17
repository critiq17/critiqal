package org.critiqal.infra.telegram;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import org.jboss.logging.Logger;


@ApplicationScoped
public class TelegramClient {

    private static final Logger log = Logger.getLogger(TelegramClient.class);
    private static final String API_BASE = "https://api.telegram.org/bot";

    @ConfigProperty(name = "telegram.bot-token")
    Optional<String> botToken;

    @ConfigProperty(name = "telegram.chat-id")
    Optional<String> chatId;

    @ConfigProperty(name = "telegram.enabled", defaultValue = "false")
    boolean enabled;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public void sendMessage(String text) {
        if (!enabled || botToken.isEmpty() || chatId.isEmpty()) {
            log.debugf("Telegram disabled or not configured, skipping: %s", text);
            return;
        }
        try {
            var encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            var url = API_BASE + botToken.get()
                    + "/sendMessage?chat_id=" + chatId.get()
                    + "&text=" + encoded
                    + "&parse_mode=HTML";
            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .whenComplete((resp, ex) -> {
                        if (ex != null) {
                            log.warnf("Telegram send failed: %s", ex.getMessage());
                        } else if (resp.statusCode() != 200) {
                            log.warnf("Telegram non-200: %s - %s",
                                    resp.statusCode(), resp.body());
                        }
                    });
        } catch (Exception e) {
            log.warnf("Telegram client error: %s", e.getMessage());
        }
    }
}
