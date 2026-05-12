package org.critiqal.domain.auth.totp.service;

import org.critiqal.domain.auth.totp.TotpSetupResult;

import java.util.UUID;

public interface TotpService {

    /** Init setup. Return QR URI and one-time codes. */
    TotpSetupResult setup(UUID userId);

    /** Confirmed TOTP with first code. Activated 2FA on account. */
    void confirm(UUID userId, String totpCode);

    /** Verified TOTP code (use on login) */
    void verify(UUID userId, String totpCode);

    /** Disable 2FA (required active TOTP code) */
    void disable(UUID userID, String totpCode);

    /** Set 2FA status is enabled. */
    boolean isEnabled(UUID userId);
}
