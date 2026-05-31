package org.critiqal.domain.ban.service;

import org.critiqal.domain.ban.UserBan;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public interface BanService {
    UserBan ban(UUID userId, UUID adminId, String reason, Duration duration);
    boolean unban(UUID userId, UUID adminId);
    Optional<UserBan> activeBan(UUID userId);
    boolean isBanned(UUID userId);
}
