package org.critiqal.domain.activity;

import java.util.UUID;

public record ActivityEvent(UUID userId, ActivityType type) {

    public enum ActivityType {
        POST_CREATED,
        COMMENT_ADDED,
        POST_LIKED,
        POST_UNLIKED,
        EVENT_HOSTED,
        EVENT_ATTENDED
    }
}
