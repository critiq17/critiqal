package org.critiqal.api.auth.response;

import java.util.List;

public record TotpSetupResponse(String qrCodeUri, String secret, List<String> recoveryCodes) {}
