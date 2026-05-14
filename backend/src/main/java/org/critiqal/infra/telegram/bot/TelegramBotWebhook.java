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
    @ConfigProperty(name = "telegram.bot.app-url") String appUrl;
    @ConfigProperty(name = "telegram.bot.support-url") String supportUrl;
    @ConfigProperty(name = "telegram.bot.tg-link") Optional<String> tgLink;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)).build();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response onUpdate(String body) {
        try {
            JsonNode update = objectMapper.readTree(body);
            String text = update.path("message").path("text").asText("");
            long chatId = update.path("message").path("chat").path("id").asLong();

            if (chatId == 0 || !text.startsWith("/start")) {
                return Response.ok().build();
            }
            sendWelcome(chatId);
        } catch (Exception e) {
            log.warnf("Bot webhook error: %s", e.getMessage());
        }
        return Response.ok().build();
    }

    private void sendWelcome(long chatId) throws Exception {
        var openAppButton = tgLink
                .map(link -> Map.<String, Object>of("text", "Open App", "url", link))
                .orElseGet(() -> Map.of("text", "Open App", "web_app", Map.of("url", appUrl)));

        var keyboard = Map.of("inline_keyboard", List.of(
                List.of(
                        openAppButton,
                        Map.<String, Object>of("text", "Support", "url", supportUrl)
                )
        ));

        var payload = Map.of(
                "chat_id", chatId,
                "text", WELCOME,
                "parse_mode", "HTML",
                "reply_markup", keyboard
        );

        var req = HttpRequest.newBuilder()
                .uri(URI.create(API + botToken.orElseThrow() + "/sendMessage"))
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
}
