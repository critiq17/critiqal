package org.critiqal.domain.badge.rule;

import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.activity.UserActivityStats;
import org.critiqal.domain.badge.BadgeCode;

@ApplicationScoped
public class ScribeRule implements BadgeRule {

    @Override public BadgeCode eligibleCode() { return BadgeCode.SCRIBE; }
    @Override public Trigger trigger() { return Trigger.ACTION; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.postsCount >= 1;
    }
}

@ApplicationScoped
class OratorRule implements BadgeRule {

    @Override public BadgeCode eligibleCode() { return BadgeCode.ORATOR; }
    @Override public Trigger trigger() { return Trigger.ACTION; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.postsCount >= 50;
    }
}


@ApplicationScoped
class TribuneRule implements BadgeRule {

    @Override public BadgeCode eligibleCode() { return BadgeCode.TRIBUNE; }
    @Override public Trigger trigger() { return Trigger.ACTION; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.postsCount >= 500
                && stats.commentsCount >= 100
                && stats.likesCount >= 1_000;
    }
}

@ApplicationScoped
class CenturionRule implements BadgeRule {

    @Override public BadgeCode eligibleCode() { return BadgeCode.CENTURION; }
    @Override public Trigger trigger() { return Trigger.DAILY; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.memberDays >= 100;
    }
}


@ApplicationScoped
class GladiatorRule implements BadgeRule {

    @Override public BadgeCode eligibleCode() { return BadgeCode.GLADIATOR; }
    @Override public Trigger trigger() { return Trigger.DAILY; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.memberDays >= 365;
    }
}

@ApplicationScoped
class LegatusRule implements BadgeRule {

    @Override public BadgeCode eligibleCode() { return BadgeCode.LEGATUS; }
    @Override public Trigger trigger() { return Trigger.DAILY; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return stats.memberDays >= 730;
    }
}

@ApplicationScoped
class OriginRule implements BadgeRule {

    @Override public BadgeCode eligibleCode() { return BadgeCode.ORIGIN; }
    @Override public Trigger trigger() { return Trigger.DAILY; }

    @Override
    public boolean isMet(UserActivityStats stats) {
        return true;
    }
}