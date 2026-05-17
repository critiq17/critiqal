package org.critiqal.infra.telegram.bot;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramBotWebhookTest {

    @Test
    void onUpdate_telegramDisabled_returnsOkWithoutRequiringConfig() {
        var webhook = new TelegramBotWebhook();
        webhook.enabled = false;

        var response = webhook.onUpdate("""
                {
                  "message": {
                    "text": "/start",
                    "chat": {
                      "id": 123
                    }
                  }
                }
                """, null);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void onUpdate_invalidSecret_returnsForbidden() {
        var webhook = new TelegramBotWebhook();
        webhook.enabled = true;
        webhook.botToken = Optional.of("token");
        webhook.supportUrl = Optional.of("https://t.me/support");
        webhook.tgLink = Optional.of("https://t.me/critiqa1_bot?startapp=start");
        webhook.webhookSecret = Optional.of("expected-secret");

        var response = webhook.onUpdate("""
                {
                  "message": {
                    "text": "/start",
                    "chat": {
                      "id": 123
                    }
                  }
                }
                """, "wrong-secret");

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void onUpdate_secretNotConfigured_allowsNonStartUpdate() {
        var webhook = new TelegramBotWebhook();
        webhook.enabled = true;
        webhook.botToken = Optional.of("token");
        webhook.supportUrl = Optional.of("https://t.me/support");
        webhook.tgLink = Optional.of("https://t.me/critiqa1_bot?startapp=start");
        webhook.webhookSecret = Optional.empty();

        var response = webhook.onUpdate("""
                {
                  "message": {
                    "text": "/noop",
                    "chat": {
                      "id": 123
                    }
                  }
                }
                """, null);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void buildOpenAppButton_prefersWebAppWhenAppUrlExists() {
        var button = TelegramBotWebhook.buildOpenAppButton(new TelegramBotWebhook.WelcomeConfig(
                "token",
                Optional.of("https://critiqal.xyz"),
                "https://t.me/critiq1",
                Optional.of("https://t.me/critiq1/app"),
                Optional.empty()
        ));

        assertThat(button).isEqualTo(Map.of(
                "text", "Open App",
                "web_app", Map.of("url", "https://critiqal.xyz")
        ));
    }

    @Test
    void buildOpenAppButton_fallsBackToPlainUrlWhenAppUrlMissing() {
        var button = TelegramBotWebhook.buildOpenAppButton(new TelegramBotWebhook.WelcomeConfig(
                "token",
                Optional.empty(),
                "https://t.me/critiq1",
                Optional.of("https://t.me/critiq1/app"),
                Optional.empty()
        ));

        assertThat(button).isEqualTo(Map.of(
                "text", "Open App",
                "url", "https://t.me/critiq1/app"
        ));
    }
}
