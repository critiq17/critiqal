package org.critiqal.domain.auth.recovery.service;

import java.util.List;
import java.util.UUID;

/**
 * Reset account access
 * Few variants:
 * 1. Email -> magic link -> new password
 * 2. Reservation code (bypass 2FA)
 * 3. Regenerate codes for authorize user
 */

public interface AccountRecoveryService {
    /** Request password reset. Always 200 - security of email enumeration. */
    void requestPasswordReset(String email);

    /** Reset password by token from email message. */
    void resetPassword(String rawToken, String newPassword);

    /**
     * Use reserve code for login (bypass 2FA)
     * @return userId for create link
     */
    UUID useRecoveryCode(String username, String rawCode);

    /** Regenerate codes. Return plainText one time - after only hash. */
    List<String> regenerateRecoveryCodes(UUID userId);

    long countActiveRecoveryCodes(UUID userId);
}
