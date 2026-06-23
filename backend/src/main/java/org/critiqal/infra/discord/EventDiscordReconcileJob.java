package org.critiqal.infra.discord;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Safety net for the after-commit sync: picks up any published event whose
 * Discord scheduled-event was never created (process crash, transient HTTP
 * failure) and finishes the job. Each id is synced through the CDI proxy so
 * the @Transactional boundary applies; sync itself is idempotent.
 */
@ApplicationScoped
public class EventDiscordReconcileJob {

    private static final Logger log = Logger.getLogger(EventDiscordReconcileJob.class);

    private final EventDiscordSyncTask task;
    private final DiscordClient discord;

    public EventDiscordReconcileJob(EventDiscordSyncTask task, DiscordClient discord) {
        this.task = task;
        this.discord = discord;
    }

    @Scheduled(every = "60s", delayed = "20s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void tick() {
        if (!discord.isConfigured()) {
            return;
        }
        try {
            // create scheduled events that never got synced
            for (UUID id : task.pendingDiscordIds()) {
                try {
                    task.syncPublished(id);
                } catch (RuntimeException e) {
                    log.warnf("Discord create reconcile failed for event %s: %s", id, e.getMessage());
                }
            }
            // remove scheduled events of cancelled events that linger in Discord
            for (UUID id : task.staleDiscordIds()) {
                try {
                    task.syncCancelled(id);
                } catch (RuntimeException e) {
                    log.warnf("Discord delete reconcile failed for event %s: %s", id, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warnf("EventDiscordReconcileJob failed: %s", e.getMessage());
        }
    }
}
