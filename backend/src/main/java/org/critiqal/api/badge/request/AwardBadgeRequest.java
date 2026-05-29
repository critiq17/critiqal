package org.critiqal.api.badge.request;

import org.critiqal.domain.badge.BadgeCode;

import java.util.Map;

public record AwardBadgeRequest(BadgeCode code, Map<String, Object> metadata) {}
