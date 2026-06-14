package org.critiqal.domain.badge.rule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class BadgeRuleRegistry {

    private final List<BadgeRule> rules;

    @Inject
    public BadgeRuleRegistry(@Any Instance<BadgeRule> instances) {
        this.rules = StreamSupport
                .stream(instances.spliterator(), false)
                .toList();
    }

    public List<BadgeRule> all() {
        return rules;
    }

    public List<BadgeRule> actionTriggered() {
        return rules.stream()
                .filter(r -> r.trigger() == BadgeRule.Trigger.ACTION)
                .toList();
    }

    public List<BadgeRule> dailyTriggered() {
        return rules.stream()
                .filter(r -> r.trigger() == BadgeRule.Trigger.DAILY)
                .toList();
    }
}
