package org.critiqal.domain.event.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.repository.UserBadgeRepository;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * "Anarchy + light, rotating hierarchy": the right to create events is a capability
 * carried by the organizer badges. AEDILE is auto-granted by reputation (see AedileRule)
 * or granted manually by an admin via the existing badge-grant panel. PRAETOR/CONSUL are
 * earned by hosting and also carry the capability.
 */
@ApplicationScoped
public class EventPermissionService {

    private static final Set<BadgeCode> ORGANIZER_BADGES =
            EnumSet.of(BadgeCode.AEDILE, BadgeCode.PRAETOR, BadgeCode.CONSUL);

    private final UserBadgeRepository userBadgeRepo;

    public EventPermissionService(UserBadgeRepository userBadgeRepo) {
        this.userBadgeRepo = userBadgeRepo;
    }

    public boolean canCreateEvents(UUID userId) {
        if (userId == null) {
            return false;
        }
        return ORGANIZER_BADGES.stream()
                .anyMatch(code -> userBadgeRepo.existsByUserIdAndBadgeCode(userId, code.name()));
    }
}
