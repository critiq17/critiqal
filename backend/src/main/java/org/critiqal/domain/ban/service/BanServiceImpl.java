package org.critiqal.domain.ban.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.auth.session.SessionService;
import org.critiqal.domain.ban.UserBan;
import org.critiqal.domain.ban.repository.UserBanRepository;
import org.critiqal.domain.shared.exception.ConflictException;
import org.critiqal.domain.shared.exception.DomainException;
import org.critiqal.domain.user.service.UserService;
import org.critiqal.infra.auth.ban.BanCache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class BanServiceImpl implements BanService {

    private final UserBanRepository banRepo;
    private final UserService userService;
    private final SessionService sessions;
    private final BanCache banCache;

    public BanServiceImpl(UserBanRepository banRepo,
                          UserService userService,
                          SessionService sessions,
                          BanCache banCache) {
        this.banRepo = banRepo;
        this.userService = userService;
        this.sessions = sessions;
        this.banCache = banCache;
    }

    @Override
    @Transactional
    public UserBan ban(UUID userId, UUID adminId, String reason, Duration duration) {
        if (reason == null || reason.isBlank()) {
            throw new DomainException("Ban reason is required");
        }
        var user = userService.getById(userId);

        banRepo.findActiveByUserId(userId).ifPresent(existing -> {
            if (existing.isActive()) {
                throw new ConflictException("User is already banned");
            }
        });

        var ban = new UserBan();
        ban.user = user;
        ban.reason = reason;
        ban.bannedBy = adminId;
        ban.expiresAt = duration != null ? Instant.now().plus(duration) : null;
        banRepo.save(ban);

        sessions.revokeAll(userId);
        banCache.invalidate(userId);
        return ban;
    }

    @Override
    @Transactional
    public boolean unban(UUID userId, UUID adminId) {
        var active = banRepo.findActiveByUserId(userId).orElse(null);
        if (active == null) return false;
        active.liftedAt = Instant.now();
        active.liftedBy = adminId;
        banCache.invalidate(userId);
        return true;
    }

    @Override
    public Optional<UserBan> activeBan(UUID userID) {
        return banRepo.findActiveByUserId(userID).filter(UserBan::isActive);
    }

    @Override
    public boolean isBanned(UUID userId) {
        return activeBan(userId).isPresent();
    }
}
