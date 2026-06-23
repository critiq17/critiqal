package org.critiqal.domain.event;

public enum EventStatus {
    DRAFT,       // visible only to host
    PUBLISHED,   // announced, RSVPs open, not started yet
    LIVE,        // started, in progress
    ENDED,       // finished — rewards granted
    CANCELLED    // called off by host
}
