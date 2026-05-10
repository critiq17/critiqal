package org.critiqal.api.auth.request;

public record RecoveryCodeRequest(String username, String recoveryCode) {}
