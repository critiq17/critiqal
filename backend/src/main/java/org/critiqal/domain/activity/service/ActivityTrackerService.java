package org.critiqal.domain.activity.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.ActivityEvent;
import org.critiqal.domain.activity.repository.UserActivityStatsRepository;
import org.critiqal.domain.badge.event.BadgeCheckEvent;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ActivityTrackerService {

    private static final Logger log = Logger.getLogger(ActivityTrackerService.class);

    private final UserActivityStatsRepository statsRepo;
    private final Event<BadgeCheckEvent> badgeCheckEvent;

    public ActivityTrackerService(UserActivityStatsRepository statsRepo,
                                  Event<BadgeCheckEvent> badgeCheckEvent) {
        this.statsRepo = statsRepo;
        this.badgeCheckEvent = badgeCheckEvent;
    }

    @Transactional
    public void onActivity(@ObservesAsync ActivityEvent event) {
        try {
            switch (event.type()) {
                case POST_CREATED -> statsRepo.incrementPosts(event.userId());
                case COMMENT_ADDED -> statsRepo.incrementComments(event.userId());
                case POST_LIKED -> statsRepo.incrementLikes(event.userId(), +1);
                case POST_UNLIKED -> statsRepo.incrementLikes(event.userId(), -1);
                case EVENT_HOSTED -> statsRepo.incrementEventsHosted(event.userId());
                case EVENT_ATTENDED -> statsRepo.incrementEventsAttended(event.userId());
            }
            badgeCheckEvent.fireAsync(new BadgeCheckEvent(event.userId(), BadgeCheckEvent.Reason.ACTION));
        } catch (Exception e) {
            log.warnf("ActivityTracker failed for userId=%s type=%s: %s",
                    event.userId(), event.type(), e.getMessage());
        }
    }
}
