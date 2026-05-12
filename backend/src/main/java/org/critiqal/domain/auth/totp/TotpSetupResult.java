package org.critiqal.domain.auth.totp;

import java.util.List;

public record TotpSetupResult(
        String qrCodeUri,
        String secret,
        List<String> recoveryCodes
) {}
