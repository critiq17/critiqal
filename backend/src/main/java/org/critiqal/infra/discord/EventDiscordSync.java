package org.critiqal.infra.discord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import org.critiqal.domain.event.event.EventCancelledEvent;
import org.critiqal.domain.event.event.EventPublishedEvent;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import java.util.UUID;

/**
 * Bridges Critiqal -> Discord once the publishing transaction commits.
 *
 * AFTER_SUCCESS fires after commit, so the row is visible. The work runs on an
 * executor that propagates NOTHING: a propagated CDI context would share the
 * request's Hibernate session across threads and corrupt it. The task opens its
 * own request context + transaction (see @ActivateRequestContext there).
 * Failures are non-fatal — the reconcile job retries any unsynced event.
 */
@ApplicationScoped
public class EventDiscordSync {

    private static final Logger log = Logger.getLogger(EventDiscordSync.class);

    private final EventDiscordSyncTask task;
    private final ManagedExecutor executor;

    public EventDiscordSync(EventDiscordSyncTask task) {
        this.task = task;
        this.executor = ManagedExecutor.builder()
                .propagated()
                .cleared(ThreadContext.ALL_REMAINING)
                .build();
    }

    void onPublished(@Observes(during = TransactionPhase.AFTER_SUCCESS) EventPublishedEvent e) {
        run("publish", e.eventId(), () -> task.syncPublished(e.eventId()));
    }

    void onCancelled(@Observes(during = TransactionPhase.AFTER_SUCCESS) EventCancelledEvent e) {
        run("cancel", e.eventId(), () -> task.syncCancelled(e.eventId()));
    }

    // AFTER_SUCCESS runs on the request thread post-commit. This must never throw:
    // a thrown observer turns an already-committed cancel/publish into a 500.
    // Reconcile retries anything that does not get synced here.
    private void run(String op, UUID id, Runnable action) {
        try {
            executor.execute(() -> {
                try {
                    action.run();
                } catch (RuntimeException ex) {
                    log.errorf("Discord %s failed for event %s: %s — reconcile will retry", op, id, ex.getMessage());
                }
            });
        } catch (RuntimeException ex) {
            log.errorf("Discord %s could not be scheduled for event %s: %s — reconcile will retry", op, id, ex.getMessage());
        }
    }
}
