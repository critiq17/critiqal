package org.critiqal.infra.telegram.bot;

import org.junit.jupiter.api.Test;

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
}
