package org.critiqal.api.badge.response;

import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.UserBadge;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UserBadgeDTO(
        UUID id,
        BadgeCode code,
        String name,
        String description,
        String iconUrl,
        Map<String, Object> metadata,
        Instant awardedAt
) {
    public static UserBadgeDTO from(UserBadge ub) {
        return new UserBadgeDTO(
                ub.id,
                BadgeCode.valueOf(ub.badge.code),
                ub.badge.name,
                ub.badge.description,
                ub.badge.iconUrl,
                ub.metadata,
                ub.awardedAt
        );
    }
}
