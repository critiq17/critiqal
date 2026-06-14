package org.critiqal.domain.badge.event;

import java.util.UUID;

public record BadgeCheckEvent(UUID userId, Reason reason) {
    public enum Reason { ACTION, DAILY }
}
