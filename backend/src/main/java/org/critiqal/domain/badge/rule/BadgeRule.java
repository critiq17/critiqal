package org.critiqal.domain.badge.rule;

import org.critiqal.domain.activity.UserActivityStats;
import org.critiqal.domain.badge.BadgeCode;

public interface BadgeRule {

    BadgeCode eligibleCode();

    boolean isMet(UserActivityStats stats);

    Trigger trigger();

    enum Trigger { ACTION, DAILY }
}
