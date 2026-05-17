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
                """);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void buildOpenAppButton_prefersWebAppWhenAppUrlExists() {
        var button = TelegramBotWebhook.buildOpenAppButton(new TelegramBotWebhook.WelcomeConfig(
                "token",
                Optional.of("https://critiqal.xyz"),
                "https://t.me/critiq1",
                Optional.of("https://t.me/critiq1/app")
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
                Optional.of("https://t.me/critiq1/app")
        ));

        assertThat(button).isEqualTo(Map.of(
                "text", "Open App",
                "url", "https://t.me/critiq1/app"
        ));
    }
}
