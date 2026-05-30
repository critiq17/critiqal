package org.critiqal.domain.badge.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.api.badge.response.UserBadgeDTO;
import org.critiqal.domain.badge.Badge;
import org.critiqal.domain.badge.BadgeCode;
import org.critiqal.domain.badge.UserBadge;
import org.critiqal.domain.badge.repository.BadgeRepository;
import org.critiqal.domain.badge.repository.UserBadgeRepository;
import org.critiqal.domain.shared.exception.NotFoundException;
import org.critiqal.domain.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class BadgeServiceImpl implements BadgeService {

    private final BadgeRepository badgeRepo;
    private final UserBadgeRepository ubRepo;
    private final UserService userService;

    public BadgeServiceImpl(BadgeRepository badgeRepo,
                            UserBadgeRepository ubRepo,
                            UserService userService) {
        this.badgeRepo = badgeRepo;
        this.ubRepo = ubRepo;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void awardBadge(UUID userId, BadgeCode code, Map<String, Object> metadata) {
        if (ubRepo.existsByUserIdAndBadgeCode(userId, code.name())) {
            return;
        }

        var user = userService.getById(userId);
        var badge = badgeRepo.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Badge not found: " + code));

        var userBadge = new UserBadge();
        userBadge.user = user;
        userBadge.badge = badge;
        userBadge.metadata = metadata;

        ubRepo.save(userBadge);
        }

    @Override
    public List<UserBadgeDTO> getUserBadges(UUID userId) {
        return ubRepo.findByUserId(userId)
                .stream()
                .map(UserBadgeDTO::from)
                .toList();
    }

    @Override
    public long countByCode(BadgeCode code) {
        return ubRepo.countByBadgeCode(code.name());
    }

    @Override
    public List<Badge> listAll() {
        return badgeRepo.listAll();
    }

    @Override
    @Transactional
    public boolean revokeBadge(UUID userId, BadgeCode code) {
        return ubRepo.deleteByUserIdAndBadgeCode(userId, code.name());
    }
}
