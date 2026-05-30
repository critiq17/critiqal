package org.critiqal.domain.badge.repository;

import org.critiqal.domain.badge.Badge;
import org.critiqal.domain.badge.BadgeCode;

import java.util.Optional;

public interface BadgeRepository {
    Optional<Badge> findByCode(BadgeCode code);
}
