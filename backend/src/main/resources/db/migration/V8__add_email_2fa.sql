ALTER TABLE users
    ADD COLUMN email VARCHAR(255),
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX idx_users_email
    ON users (email)
    WHERE email IS NOT NULL;

CREATE TABLE verification_tokens (
    id  UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_verification_tokens PRIMARY KEY (id),
    CONSTRAINT fk_vt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_vt_type CHECK (type IN ('EMAIL_VERIFY', 'PASSWORD_RESET'))
);
CREATE UNIQUE INDEX idx_vt_token_hash ON verification_tokens (token_hash);
CREATE INDEX idx_vt_user_type ON verification_tokens (user_id, type);

CREATE TABLE recovery_codes (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_recovery_codes PRIMARY KEY (id),
    CONSTRAINT fk_rc_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_rc_user_id ON recovery_codes (user_id);

CREATE TABLE totp_secrets (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    secret_encrypted VARCHAR(512) NOT NULL,
    confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_totp_secrets PRIMARY KEY (id),
    CONSTRAINT fk_ts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);