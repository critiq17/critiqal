package org.critiqal.domain.badge.service;

import org.critiqal.api.badge.response.UserBadgeDTO;
import org.critiqal.domain.badge.BadgeCode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface BadgeService {
    /**
     *  Idempotent: ignored if badge exists
     */
    void awardBadge(UUID userId, BadgeCode code, Map<String, Object> metadata);

    List<UserBadgeDTO> getUserBadges(UUID userId);

    long countByCode(BadgeCode code);
}
