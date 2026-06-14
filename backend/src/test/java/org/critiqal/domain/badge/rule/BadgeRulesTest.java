package org.critiqal.domain.badge.rule;

import org.critiqal.domain.activity.UserActivityStats;
import org.critiqal.domain.badge.BadgeCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for all badge rules — no CDI, no DB, no mocks needed.
 * Each test builds a UserActivityStats struct and asserts isMet().
 */
class BadgeRulesTest {

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static UserActivityStats stats(long posts, long comments, long likes, int days) {
        var s = new UserActivityStats();
        s.userId = UUID.randomUUID();
        s.postsCount = posts;
        s.commentsCount = comments;
        s.likesCount = likes;
        s.memberDays = days;
        return s;
    }

    // ── Scribe ────────────────────────────────────────────────────────────────

    @Test
    void scribe_notMet_whenNoPosts() {
        assertThat(new ScribeRule().isMet(stats(0, 0, 0, 5))).isFalse();
    }

    @Test
    void scribe_met_onFirstPost() {
        assertThat(new ScribeRule().isMet(stats(1, 0, 0, 5))).isTrue();
    }

    // ── Orator ────────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({"0", "1", "49"})
    void orator_notMet_belowThreshold(int posts) {
        assertThat(new OratorRule().isMet(stats(posts, 0, 0, 10))).isFalse();
    }

    @Test
    void orator_met_atExactly50Posts() {
        assertThat(new OratorRule().isMet(stats(50, 0, 0, 10))).isTrue();
    }

    // ── Tribune ───────────────────────────────────────────────────────────────

    @Test
    void tribune_notMet_missingComments() {
        assertThat(new TribuneRule().isMet(stats(500, 99, 1000, 100))).isFalse();
    }

    @Test
    void tribune_notMet_missingLikes() {
        assertThat(new TribuneRule().isMet(stats(500, 100, 999, 100))).isFalse();
    }

    @Test
    void tribune_met_allThresholdsReached() {
        assertThat(new TribuneRule().isMet(stats(500, 100, 1000, 100))).isTrue();
    }

    // ── Centurion ─────────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({"0", "50", "99"})
    void centurion_notMet_belowThreshold(int days) {
        assertThat(new CenturionRule().isMet(stats(0, 0, 0, days))).isFalse();
    }

    @Test
    void centurion_met_atExactly100Days() {
        assertThat(new CenturionRule().isMet(stats(0, 0, 0, 100))).isTrue();
    }

    // ── Gladiator ─────────────────────────────────────────────────────────────

    @Test
    void gladiator_notMet_at364Days() {
        assertThat(new GladiatorRule().isMet(stats(0, 0, 0, 364))).isFalse();
    }

    @Test
    void gladiator_met_at365Days() {
        assertThat(new GladiatorRule().isMet(stats(0, 0, 0, 365))).isTrue();
    }

    // ── Legatus ───────────────────────────────────────────────────────────────

    @Test
    void legatus_notMet_at729Days() {
        assertThat(new LegatusRule().isMet(stats(0, 0, 0, 729))).isFalse();
    }

    @Test
    void legatus_met_at730Days() {
        assertThat(new LegatusRule().isMet(stats(0, 0, 0, 730))).isTrue();
    }

    // ── Trigger type checks ───────────────────────────────────────────────────

    @Test
    void actionTriggeredRules_areScribeOratorTribune() {
        var action = java.util.List.of(
                new ScribeRule(), new OratorRule(), new TribuneRule());
        action.forEach(r -> assertThat(r.trigger())
                .as("%s should be ACTION-triggered", r.getClass().getSimpleName())
                .isEqualTo(BadgeRule.Trigger.ACTION));
    }

    @Test
    void dailyTriggeredRules_areTenureAndOrigin() {
        var daily = java.util.List.of(
                new CenturionRule(), new GladiatorRule(), new LegatusRule(), new OriginRule());
        daily.forEach(r -> assertThat(r.trigger())
                .as("%s should be DAILY-triggered", r.getClass().getSimpleName())
                .isEqualTo(BadgeRule.Trigger.DAILY));
    }

    // ── Eligibility codes ─────────────────────────────────────────────────────

    @Test
    void eachRuleReturnsCorrectBadgeCode() {
        assertThat(new ScribeRule().eligibleCode()).isEqualTo(BadgeCode.SCRIBE);
        assertThat(new OratorRule().eligibleCode()).isEqualTo(BadgeCode.ORATOR);
        assertThat(new TribuneRule().eligibleCode()).isEqualTo(BadgeCode.TRIBUNE);
        assertThat(new CenturionRule().eligibleCode()).isEqualTo(BadgeCode.CENTURION);
        assertThat(new GladiatorRule().eligibleCode()).isEqualTo(BadgeCode.GLADIATOR);
        assertThat(new LegatusRule().eligibleCode()).isEqualTo(BadgeCode.LEGATUS);
        assertThat(new OriginRule().eligibleCode()).isEqualTo(BadgeCode.ORIGIN);
    }
}