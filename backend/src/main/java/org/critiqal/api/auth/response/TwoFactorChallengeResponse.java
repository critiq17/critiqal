package org.critiqal.api.auth.response;

public record TwoFactorChallengeResponse(String challengeToken, String method) {}
