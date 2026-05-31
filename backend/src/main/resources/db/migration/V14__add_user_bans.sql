CREATE TABLE user_bans (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    reason TEXT NOT NULL,
    banned_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    lifted_at TIMESTAMP WITH TIME ZONE,
    lifted_by UUID,
    CONSTRAINT pk_user_bans PRIMARY KEY (id),
    CONSTRAINT fk_ub_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ub_banned_by FOREIGN KEY (banned_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX idx_user_bans_active
    ON user_bans (user_id)
    WHERE lifted_at IS NULL;

CREATE INDEX idx_user_bans_user ON user_bans (user_id, created_at DESC);