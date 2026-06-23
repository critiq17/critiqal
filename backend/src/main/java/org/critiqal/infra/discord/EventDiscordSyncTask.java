package org.critiqal.infra.discord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.transaction.Transactional;
import org.critiqal.domain.event.Event;
import org.critiqal.domain.event.EventStatus;
import org.critiqal.domain.event.repository.EventRepository;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class EventDiscordSyncTask {

    private static final Logger log = Logger.getLogger(EventDiscordSyncTask.class);
    private static final int RECONCILE_BATCH = 20;
    private static final long DEFAULT_DURATION_SECONDS = 3600;

    private final EventRepository eventRepo;
    private final DiscordClient discord;
    private final String eventBaseUrl;

    public EventDiscordSyncTask(
            EventRepository eventRepo,
            DiscordClient discord,
            @ConfigProperty(name = "discord.app-event-base-url", defaultValue = "http://localhost:5173/events") String eventBaseUrl) {
        this.eventRepo = eventRepo;
        this.discord = discord;
        this.eventBaseUrl = eventBaseUrl;
    }

    /** Creates the Discord scheduled event + announcement. Idempotent on discordEventId. */
    @ActivateRequestContext
    @Transactional
    public void syncPublished(UUID eventId) {
        eventRepo.findByIdOptional(eventId).ifPresent(this::create);
    }

    @ActivateRequestContext
    @Transactional
    public void syncCancelled(UUID eventId) {
        var event = eventRepo.findByIdOptional(eventId).orElse(null);
        if (event == null || event.discordEventId == null) {
            return;
        }
        // only forget the id once Discord confirms removal; otherwise reconcile retries
        if (discord.deleteScheduledEvent(event.discordEventId)) {
            event.discordEventId = null;
        }
    }

    /** Published/live events still missing their Discord scheduled-event. */
    @Transactional
    public List<UUID> pendingDiscordIds() {
        return eventRepo.findPendingDiscordSync(RECONCILE_BATCH).stream()
                .map(e -> e.id)
                .toList();
    }

    /** Cancelled events whose Discord scheduled-event was never removed. */
    @Transactional
    public List<UUID> staleDiscordIds() {
        return eventRepo.findStaleDiscordEvents(RECONCILE_BATCH).stream()
                .map(e -> e.id)
                .toList();
    }

    // Only acts when the event is live-bound and not yet synced. A failed
    // createScheduledEvent leaves discordEventId null, so reconcile retries it;
    // the announcement fires exactly once, alongside successful creation.
    private void create(Event event) {
        if (!discord.isConfigured() || event.discordEventId != null) {
            return;
        }
        if (event.status != EventStatus.PUBLISHED && event.status != EventStatus.LIVE) {
            return;
        }

        String endTime = event.endsAt != null
                ? event.endsAt.toString()
                : event.startsAt.plusSeconds(DEFAULT_DURATION_SECONDS).toString();

        var discordId = discord.createScheduledEvent(
                event.title,
                event.description,
                event.startsAt.toString(),
                endTime,
                discordLocation(event));

        if (discordId.isEmpty()) {
            log.warnf("Discord scheduled-event creation failed for event %s; reconcile will retry", event.id);
            return;
        }

        event.discordEventId = discordId.get();
        event.discordGuildId = discord.guildId();
        discord.announce(event, eventBaseUrl + "/" + event.id);
        log.infof("Discord sync complete for event %s (discordEventId=%s)", event.id, event.discordEventId);
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
