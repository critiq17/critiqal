package org.critiqal.api.admin.response;

import org.critiqal.api.badge.response.UserBadgeDTO;
import org.critiqal.domain.ban.UserBan;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// Lightweight user row for the admin dashboard: identity + current badges + ban status.
public record AdminUserDTO(
        UUID id,
        String username,
        String name,
        String avatarUrl,
        List<UserBadgeDTO> badges,
        boolean banned,
        String bannedUntil  // ISO string or null
) {
    public static AdminUserDTO from(User user, List<UserBadgeDTO> badges, Optional<UserBan> activeBan) {
        boolean isBanned = activeBan.isPresent();
        String until = activeBan
                .map(b -> b.expiresAt != null ? b.expiresAt.toString() : null)
                .orElse(null);
        return new AdminUserDTO(user.id, user.username, user.name, user.avatarUrl, badges, isBanned, until);
    }
}
