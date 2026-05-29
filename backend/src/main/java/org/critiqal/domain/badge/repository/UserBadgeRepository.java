package org.critiqal.domain.badge.repository;

import org.critiqal.domain.badge.UserBadge;

import java.util.List;
import java.util.UUID;

public interface UserBadgeRepository {
    boolean existsByUserIdAndBadgeCode(UUID userId, String code);

    List<UserBadge> findByUserId(UUID userId);

    long countByBadgeCode(String code);

    UserBadge save(UserBadge userBadge);
}
