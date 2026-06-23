CREATE TABLE events (
    id                UUID         NOT NULL,
    host_id           UUID         NOT NULL,
    title             VARCHAR(140) NOT NULL,
    description       TEXT,
    cover_image_url   VARCHAR(512),
    location_type     VARCHAR(24)  NOT NULL DEFAULT 'DISCORD',
    location_value    VARCHAR(512),
    starts_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at           TIMESTAMP WITH TIME ZONE,
    capacity          INT,
    attendee_count    INT          NOT NULL DEFAULT 0,
    status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
    discord_guild_id  VARCHAR(32),
    discord_event_id  VARCHAR(32),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_events PRIMARY KEY (id),
    CONSTRAINT fk_events_host FOREIGN KEY (host_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_events_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT chk_events_attendees CHECK (attendee_count >= 0)
);

CREATE INDEX idx_events_status_starts ON events (status, starts_at);
CREATE INDEX idx_events_host          ON events (host_id);
CREATE INDEX idx_events_starts        ON events (starts_at);

CREATE TABLE event_attendances (
    id          UUID         NOT NULL,
    event_id    UUID         NOT NULL,
    user_id     UUID         NOT NULL,
    status      VARCHAR(16)  NOT NULL DEFAULT 'GOING',
    checked_in  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_event_attendances PRIMARY KEY (id),
    CONSTRAINT uq_event_attendance UNIQUE (event_id, user_id),
    CONSTRAINT fk_att_event FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_att_user  FOREIGN KEY (user_id)  REFERENCES users (id)  ON DELETE CASCADE
);

CREATE INDEX idx_att_event ON event_attendances (event_id, status);
CREATE INDEX idx_att_user  ON event_attendances (user_id);
