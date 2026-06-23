ALTER TABLE user_activity_stats
    ADD COLUMN events_hosted   BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN events_attended BIGINT NOT NULL DEFAULT 0;

ALTER TABLE user_activity_stats
    ADD CONSTRAINT chk_uas_events_non_negative
        CHECK (events_hosted >= 0 AND events_attended >= 0);
