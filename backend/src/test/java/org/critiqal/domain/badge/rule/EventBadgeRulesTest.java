package org.critiqal.domain.badge.rule;

import org.critiqal.domain.activity.UserActivityStats;
import org.critiqal.domain.badge.BadgeCode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for the event-organizer badge rules — no CDI, no DB.
 */
class EventBadgeRulesTest {

    private static UserActivityStats stats(long posts, long comments, long hosted) {
        var s = new UserActivityStats();
        s.userId = UUID.randomUUID();
        s.postsCount = posts;
        s.commentsCount = comments;
        s.eventsHosted = hosted;
        return s;
    }

    // ── Aedile (capability: reputation) ─────────────────────────────────────────

    @Test
    void aedile_notMet_belowPosts() {
        assertThat(new AedileRule().isMet(stats(9, 20, 0))).isFalse();
    }

    @Test
    void aedile_notMet_belowComments() {
        assertThat(new AedileRule().isMet(stats(10, 19, 0))).isFalse();
    }

    @Test
    void aedile_met_atThresholds() {
        assertThat(new AedileRule().isMet(stats(10, 20, 0))).isTrue();
    }

    // ── Praetor (5 hosted) ──────────────────────────────────────────────────────

    @Test
    void praetor_notMet_at4Hosted() {
        assertThat(new PraetorRule().isMet(stats(0, 0, 4))).isFalse();
    }

    @Test
    void praetor_met_at5Hosted() {
        assertThat(new PraetorRule().isMet(stats(0, 0, 5))).isTrue();
    }

    // ── Consul (20 hosted) ──────────────────────────────────────────────────────

    @Test
    void consul_notMet_at19Hosted() {
        assertThat(new ConsulRule().isMet(stats(0, 0, 19))).isFalse();
    }

    @Test
    void consul_met_at20Hosted() {
        assertThat(new ConsulRule().isMet(stats(0, 0, 20))).isTrue();
    }

    // ── Trigger + code wiring ───────────────────────────────────────────────────

    @Test
    void allOrganizerRules_areActionTriggered() {
        assertThat(new AedileRule().trigger()).isEqualTo(BadgeRule.Trigger.ACTION);
        assertThat(new PraetorRule().trigger()).isEqualTo(BadgeRule.Trigger.ACTION);
        assertThat(new ConsulRule().trigger()).isEqualTo(BadgeRule.Trigger.ACTION);
    }

    @Test
    void eachRuleReturnsCorrectBadgeCode() {
        assertThat(new AedileRule().eligibleCode()).isEqualTo(BadgeCode.AEDILE);
        assertThat(new PraetorRule().eligibleCode()).isEqualTo(BadgeCode.PRAETOR);
        assertThat(new ConsulRule().eligibleCode()).isEqualTo(BadgeCode.CONSUL);
    }
}
