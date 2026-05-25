CREATE TABLE auth_sessions (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    session_id_hash VARCHAR(64) NOT NULL,
    device_id_hash VARCHAR(64),
    ip_hash VARCHAR(64),
    country_code VARCHAR(2),
    city VARCHAR(128),
    user_agent TEXT,
    platform VARCHAR(32),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_auth_sessions PRIMARY KEY (id),
    CONSTRAINT fk_auth_sessions_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_auth_sessions_sid_hash
    ON auth_sessions (session_id_hash);

CREATE INDEX idx_auth_sessions_user_seen
    ON auth_sessions (user_id, last_seen_at DESC);

CREATE INDEX idx_auth_sessions_device_hash
    ON auth_sessions (device_id_hash)
    WHERE device_id_hash IS NOT NULL;
