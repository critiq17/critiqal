package org.critiqal.infra.discord;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin Discord REST client (API v10) using bot-token auth.
 * Mirrors the outbound-HTTP style of the existing Telegram/Strava infra.
 */
@ApplicationScoped
public class DiscordClient {

    private static final Logger log = Logger.getLogger(DiscordClient.class);
    private static final String API = "https://discord.com/api/v10";

    // Discord scheduled-event constants
    private static final int PRIVACY_GUILD_ONLY = 2;
    private static final int ENTITY_EXTERNAL = 3;
    private static final int ACCENT_COLOR = 0xE05252; // matches the app accent

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "discord.enabled", defaultValue = "false")
    boolean enabled;
    @ConfigProperty(name = "discord.bot-token", defaultValue = "")
    String botToken;
    @ConfigProperty(name = "discord.guild-id", defaultValue = "")
    String guildId;
    @ConfigProperty(name = "discord.announce-webhook-url", defaultValue = "")
    String webhookUrl;

    public boolean isConfigured() {
        return enabled && !botToken.isBlank() && !guildId.isBlank();
    }

    public String guildId() {
        return guildId;
    }

    /** Creates a Discord Guild Scheduled Event; returns its id, or empty on failure/disabled. */
    public Optional<String> createScheduledEvent(String name,
                                                 String description,
                                                 String startIso,
                                                 String endIso,
                                                 String location) {
        if (!isConfigured()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("name", truncate(name, 100));
            body.put("privacy_level", PRIVACY_GUILD_ONLY);
            body.put("scheduled_start_time", startIso);
            body.put("scheduled_end_time", endIso); // required for EXTERNAL events
            body.put("entity_type", ENTITY_EXTERNAL);
            body.put("entity_metadata", Map.of("location",
                    truncate(location == null || location.isBlank() ? "Critiqal" : location, 100)));
            if (description != null && !description.isBlank()) {
                body.put("description", truncate(description, 1000));
            }

            var req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/guilds/" + guildId + "/scheduled-events"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bot " + botToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warnf("Discord createScheduledEvent failed: %d %s", resp.statusCode(), resp.body());
                return Optional.empty();
            }
            Map<?, ?> json = mapper.readValue(resp.body(), Map.class);
            return Optional.ofNullable((String) json.get("id"));
        } catch (Exception e) {
            log.warnf("Discord createScheduledEvent error: %s", e.getMessage());
            return Optional.empty();
        }
    }

    public void deleteScheduledEvent(String discordEventId) {
        if (!isConfigured() || discordEventId == null) {
            return;
        }
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(API + "/guilds/" + guildId + "/scheduled-events/" + discordEventId))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bot " + botToken)
                    .DELETE()
                    .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2 && resp.statusCode() != 404) {
                log.warnf("Discord deleteScheduledEvent failed: %d %s", resp.statusCode(), resp.body());
            }
        } catch (Exception e) {
            log.warnf("Discord deleteScheduledEvent error: %s", e.getMessage());
        }
    }

    /** Posts a rich announcement embed to the configured channel webhook. */
    public void announce(String title, String description, String url, String imageUrl) {
        if (webhookUrl.isBlank()) {
            return;
        }
        try {
            Map<String, Object> embed = new HashMap<>();
            embed.put("title", truncate(title, 256));
            if (description != null && !description.isBlank()) {
                embed.put("description", truncate(description, 2048));
            }
            if (url != null && !url.isBlank()) {
                embed.put("url", url);
            }
            embed.put("color", ACCENT_COLOR);
            if (imageUrl != null && !imageUrl.isBlank()) {
                embed.put("image", Map.of("url", imageUrl));
            }

            Map<String, Object> payload = Map.of(
                    "content", "📣 New event on Critiqal",
                    "embeds", List.of(embed));

            var req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                    .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.warnf("Discord announce error: %s", e.getMessage());
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
