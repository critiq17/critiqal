package org.critiqal.domain.event;

public enum EventLocationType {
    DISCORD,    // happens in a Discord channel/server
    TELEGRAM,   // Telegram group/channel
    IRL,        // real-life meetup
    EXTERNAL    // any other link
}
