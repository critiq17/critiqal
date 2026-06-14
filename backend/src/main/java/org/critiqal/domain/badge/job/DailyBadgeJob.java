package org.critiqal.domain.badge.job;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.repository.UserActivityStatsRepository;
import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.service.BadgeService;
import org.critiqal.domain.user.repository.UserRepository;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class DailyBadgeJob {

    private static final Logger log = Logger.getLogger(DailyBadgeJob.class);

    private static final List<TenureMilestone> TENURES = List.of(
            new TenureMilestone(BadgeCode.CENTURION, 100),
            new TenureMilestone(BadgeCode.GLADIATOR, 365),
            new TenureMilestone(BadgeCode.LEGATUS, 730)
    );

    private static final int ORIGIN_THRESHOLD = 100;

    private final UserActivityStatsRepository statsRepo;
    private final BadgeService badgeService;
    private final UserRepository userRepo;

    public DailyBadgeJob(UserActivityStatsRepository statsRepo,
                         BadgeService badgeService,
                         UserRepository userRepo) {
        this.statsRepo = statsRepo;
        this.badgeService = badgeService;
        this.userRepo = userRepo;
    }

    @Scheduled(cron = "0 0 1 * * ?", timeZone = "UTC")
    @Transactional
    public void run() {
        log.info("DailyBadgeJob Starting");

        int updated = statsRepo.incrementAllMemberDays();
        log.infof("DailyBadgeJob: incremented member_days for %d users", updated);

        for (var milestones : TENURES) {
            var userIds = statsRepo.findUserIdsWithMemberDays(milestones.days());
            if (!userIds.isEmpty()) {
                log.infof("DailyBadgeJob: %d users hit %s milestones today",
                        userIds.size(), milestones.code());
            }
            for (var userId : userIds) {
                try {
                    badgeService.awardBadge(userId, milestones.code(), null);
                } catch (Exception e) {
                    log.warnf("DailyBadgeJob: failed to award %s to %s: %s",
                            milestones.code(), userId, e.getMessage());
                }
            }
        }

        evaluateOriginBadge();

        log.info("DailyBadgeJob finished");
    }

    private void evaluateOriginBadge() {
        var eligibleIds = userRepo.findEarlyUsersWithoutBadge(
                BadgeCode.ORIGIN.name(), ORIGIN_THRESHOLD);

        for (var userId : eligibleIds) {
            try {
                badgeService.awardBadge(userId, BadgeCode.ORIGIN, null);
            } catch (Exception e) {
                log.warnf("DailyBadgeJob: failed to award ORIGIN to %s: %s", userId, e.getMessage());
            }
        }
    }
    private record TenureMilestone(BadgeCode code, int days) {}
}