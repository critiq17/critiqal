CREATE TABLE strava_integrations (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    athlete_id BIGINT NOT NULL,
    athlete_username VARCHAR(255),
    athlete_firstname VARCHAR(255),
    athlete_lastname VARCHAR(255),
    athlete_city VARCHAR(255),
    athlete_avatar_url VARCHAR(255),
    access_token VARCHAR(512) NOT NULL,
    refresh_token VARCHAR(512) NOT NULL,
    expires_at BIGINT NOT NULL,
    connected_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_strava_user UNIQUE (user_id),
    CONSTRAINT uk_strava_athlete UNIQUE (athlete_id),
    CONSTRAINT fk_strava_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_strava_user_id ON strava_integrations(user_id);