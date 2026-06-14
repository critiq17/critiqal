package org.critiqal.domain.badge.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.UserActivityStats;
import org.critiqal.domain.activity.repository.UserActivityStatsRepository;
import org.critiqal.domain.badge.event.BadgeCheckEvent;
import org.critiqal.domain.badge.rule.BadgeRule;
import org.critiqal.domain.badge.rule.BadgeRuleRegistry;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class BadgeEvaluatorService {

    private static final Logger log = Logger.getLogger(BadgeEvaluatorService.class);

    private final UserActivityStatsRepository statsRepo;
    private final BadgeRuleRegistry ruleRegistry;
    private final BadgeService badgeService;

    public BadgeEvaluatorService(UserActivityStatsRepository statsRepo,
                                 BadgeRuleRegistry ruleRegistry,
                                 BadgeService badgeService) {
        this.statsRepo = statsRepo;
        this.ruleRegistry = ruleRegistry;
        this.badgeService = badgeService;
    }

    @Transactional
    public void onBadgeCheck(@ObservesAsync BadgeCheckEvent event) {
        try {
            var statsOpt = statsRepo.findByUserId(event.userId());
            if (statsOpt.isEmpty()) return;

            var stats = statsOpt.get();
            var rulesToCheck = event.reason() == BadgeCheckEvent.Reason.ACTION
                    ? ruleRegistry.actionTriggered()
                    : ruleRegistry.all();
            evaluateAndAware(stats, rulesToCheck);
        } catch (Exception e) {
            log.warnf("BadgeEvaluator failed for userId=%s: %s", event.userId(), e.getMessage());
        }
    }

    @Transactional
    public void evaluateAndAware(UserActivityStats stats, List<BadgeRule> rules) {
        for (BadgeRule rule : rules) {
            if (rule.isMet(stats)) {
                try {
                    badgeService.awardBadge(stats.userId, rule.eligibleCode(), null);
                } catch (Exception e) {
                    log.warnf("Failed to award badge %s to userId=%s: %s",
                            rule.eligibleCode(), stats.userId, e.getMessage());
                }
            }
        }
    }
}
