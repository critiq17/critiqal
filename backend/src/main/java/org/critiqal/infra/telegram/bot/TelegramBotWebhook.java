package org.critiqal.infra.telegram.bot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/api/telegram/webhook")
public class TelegramBotWebhook {

    private static final Logger log = Logger.getLogger(TelegramBotWebhook.class);
    private static final String API = "https://api.telegram.org/bot";

    private static final String WELCOME = """
            <b>Critiqal</b>
        
            A privacy social network
           
            <b>How to use?</b>
            - Open mini-app via button below
            - Sign up or log in
            - Share your results, follow others people
            
            _____
            
            Приватная социальная сеть
            
            <b>Как пользоваться?</b>
            - Открой приложения кнопкой ниже
            - Зарегестрируйся или войди
            - Делись результатами, подписывайся на других людей
            """;

    @ConfigProperty(name = "telegram.bot-token") Optional<String> botToken;
    @ConfigProperty(name = "telegram.enabled", defaultValue = "false") boolean enabled;
    @ConfigProperty(name = "telegram.bot.app-url") Optional<String> appUrl;
    @ConfigProperty(name = "telegram.bot.support-url") Optional<String> supportUrl;
    @ConfigProperty(name = "telegram.bot.tg-link") Optional<String> tgLink;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response onUpdate(String body) {
        var config = resolveConfig();
        if (config.isEmpty()) {
            return Response.ok().build();
        }
        try {
            JsonNode update = objectMapper.readTree(body);
            String text = update.path("message").path("text").asText("");
            long chatId = update.path("message").path("chat").path("id").asLong();

            if (chatId == 0 || !text.startsWith("/start")) {
                return Response.ok().build();
            }
            sendWelcome(chatId, config.get());
        } catch (Exception e) {
            log.warnf("Bot webhook error: %s", e.getMessage());
        }
        return Response.ok().build();
    }

    private Optional<WelcomeConfig> resolveConfig() {
        if (!enabled) {
            return Optional.empty();
        }
        if (botToken.isEmpty()) {
            log.warn("Telegram webhook is enabled but telegram.bot-token is missing");
            return Optional.empty();
        }
        if (supportUrl.isEmpty()) {
            log.warn("Telegram webhook is enabled but telegram.bot.support-url is missing");
            return Optional.empty();
        }
        if (tgLink.isEmpty() && appUrl.isEmpty()) {
            log.warn("Telegram webhook is enabled but neither telegram.bot.tg-link nor telegram.bot.app-url is configured");
            return Optional.empty();
        }
        if (appUrl.isEmpty() && tgLink.isPresent()) {
            log.warn("Telegram welcome button is using telegram.bot.tg-link fallback; configure telegram.bot.app-url to open the native Mini App container");
        }
        return Optional.of(new WelcomeConfig(botToken.get(), appUrl, supportUrl.get(), tgLink));
    }

    private void sendWelcome(long chatId, WelcomeConfig config) throws Exception {
        var openAppButton = buildOpenAppButton(config);

        var keyboard = Map.of("inline_keyboard", List.of(
                List.of(
                        openAppButton,
                        Map.<String, Object>of("text", "Support", "url", config.supportUrl())
                )
        ));

        var payload = Map.of(
                "chat_id", chatId,
                "text", WELCOME,
                "parse_mode", "HTML",
                "reply_markup", keyboard
        );

        var req = HttpRequest.newBuilder()
                .uri(URI.create(API + config.botToken() + "/sendMessage"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .timeout(Duration.ofSeconds(10))
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, ex) -> {
                    if (ex != null) log.warnf("Telegram send failed: %s", ex.getMessage());
                    else if (resp.statusCode() != 200) log.warnf("Telegram non-200: %s", resp.body());
                });
    }

    static Map<String, Object> buildOpenAppButton(WelcomeConfig config) {
        if (config.appUrl().isPresent()) {
            // Prefer Telegram's native web_app launch path whenever possible.
            // A plain URL button opens a browser-like sheet and skips Mini App
            // capabilities like reliable fullscreen and swipe lock.
            return Map.of("text", "Open App", "web_app", Map.of("url", config.appUrl().orElseThrow()));
        }
        return config.tgLink()
                .map(link -> Map.<String, Object>of("text", "Open App", "url", link))
                .orElseThrow(() -> new IllegalStateException("Missing Telegram app launch config"));
    }

    record WelcomeConfig(
            String botToken,
            Optional<String> appUrl,
            String supportUrl,
            Optional<String> tgLink
    ) {
    }
}
