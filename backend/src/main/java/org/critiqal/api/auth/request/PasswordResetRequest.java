package org.critiqal.api.auth.request;

public record PasswordResetRequest(String token, String newPassword) {}
