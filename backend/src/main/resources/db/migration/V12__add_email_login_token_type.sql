-- Allow EMAIL_LOGIN tokens (per-login 6-digit OTP).
ALTER TABLE verification_tokens DROP CONSTRAINT chk_vt_type;
ALTER TABLE verification_tokens
    ADD CONSTRAINT chk_vt_type
    CHECK (type IN ('EMAIL_VERIFY', 'EMAIL_LOGIN', 'PASSWORD_RESET'));
