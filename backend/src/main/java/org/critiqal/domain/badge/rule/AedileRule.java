package org.critiqal.domain.badge.rule;

import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.activity.UserActivityStats;
import org.critiqal.domain.badge.BadgeCode;

/** AEDILE — the right to organize events, earned by community reputation. */
@ApplicationScoped
public class AedileRule implements BadgeRule {
    @Override public BadgeCode eligibleCode() { return BadgeCode.AEDILE; }
    @Override public Trigger trigger() { return Trigger.ACTION; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.postsCount >= 10 && stats.commentsCount >= 20;
    }
}

/** PRAETOR — hosted 5 community events. */
@ApplicationScoped
class PraetorRule implements BadgeRule {
    @Override public BadgeCode eligibleCode() { return BadgeCode.PRAETOR; }
    @Override public Trigger trigger() { return Trigger.ACTION; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.eventsHosted >= 5;
    }
}

/** CONSUL — hosted 20 community events. */
@ApplicationScoped
class ConsulRule implements BadgeRule {
    @Override public BadgeCode eligibleCode() { return BadgeCode.CONSUL; }
    @Override public Trigger trigger() { return Trigger.ACTION; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.eventsHosted >= 20;
    }
}
