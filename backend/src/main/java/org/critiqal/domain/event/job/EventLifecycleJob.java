package org.critiqal.domain.event.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.event.service.EventService;
import org.jboss.logging.Logger;

@ApplicationScoped
public class EventLifecycleJob {

    private static final Logger log = Logger.getLogger(EventLifecycleJob.class);

    private final EventService eventService;

    public EventLifecycleJob(EventService eventService) {
        this.eventService = eventService;
    }

    @Scheduled(every = "60s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    void tick() {
        try {
            eventService.startDueEvents();
            eventService.endDueEvents();
        } catch (Exception e) {
            log.warnf("EventLifecycleJob failed: %s", e.getMessage());
        }
    }
}
