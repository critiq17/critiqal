package org.critiqal.infra.discord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.transaction.Transactional;
import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.event.EventCancelledEvent;
import org.critiqal.domain.event.event.EventPublishedEvent;
import org.critiqal.domain.event.repository.EventRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Reactive bridge "Critiqal -> Discord". On publish it mirrors the event into a
 * Discord Guild Scheduled Event and posts a webhook announcement; on cancel it
 * deletes the scheduled event. Failures are swallowed so they never break the
 * in-app flow (the social network stays the source of truth).
 */
@ApplicationScoped
public class EventDiscordSync {

    private static final Logger log = Logger.getLogger(EventDiscordSync.class);

    private final EventRepository eventRepo;
    private final DiscordClient discord;

    @ConfigProperty(name = "discord.app-event-base-url", defaultValue = "http://localhost:5173/events")
    String eventBaseUrl;

    public EventDiscordSync(EventRepository eventRepo, DiscordClient discord) {
        this.eventRepo = eventRepo;
        this.discord = discord;
    }

    @Transactional
    void onPublished(@ObservesAsync EventPublishedEvent e) {
        var event = eventRepo.findByIdOptional(e.eventId()).orElse(null);
        if (event == null) {
            return;
        }

        String url = eventBaseUrl + "/" + event.id;

        if (discord.isConfigured() && event.discordEventId == null) {
            discord.createScheduledEvent(
                    event.title,
                    event.description,
                    event.startsAt.toString(),
                    event.endsAt.toString(),
                    discordLocation(event)
            ).ifPresent(discordId -> {
                event.discordEventId = discordId;
                event.discordGuildId = discord.guildId();
            });
        }

        discord.announce(event.title, event.description, url, event.coverImageUrl);
        log.infof("EventDiscordSync: published event %s synced (discordEventId=%s)",
                event.id, event.discordEventId);
    }

    @Transactional
    void onCancelled(@ObservesAsync EventCancelledEvent e) {
        var event = eventRepo.findByIdOptional(e.eventId()).orElse(null);
        if (event == null || event.discordEventId == null) {
            return;
        }
        discord.deleteScheduledEvent(event.discordEventId);
        event.discordEventId = null;
    }

    private String discordLocation(Event event) {
        return switch (event.locationType) {
            case DISCORD -> event.locationValue != null ? event.locationValue : "Discord";
            case TELEGRAM -> "Telegram: " + safe(event.locationValue);
            case IRL, EXTERNAL -> safe(event.locationValue);
        };
    }

    private String safe(String v) {
        return v == null ? "Critiqal" : v;
    }
}
