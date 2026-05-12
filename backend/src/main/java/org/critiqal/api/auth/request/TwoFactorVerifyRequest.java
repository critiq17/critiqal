package org.critiqal.api.auth.request;

public record TwoFactorVerifyRequest(String challengeToken, String code) {}
