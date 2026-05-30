package org.critiqal.api.user.response;

import org.critiqal.api.badge.response.UserBadgeDTO;
import org.critiqal.domain.badge.UserBadge;
import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String name,
        String bio,
        String avatarUrl,
        String email,
        List<UserBadgeDTO> badges,
        boolean emailVerified,
        String pendingEmail,
        boolean twoFactorEnabled,
        Instant createdAt,
        UserStatsDTO stats,
        Boolean isFollowing
) {
    public static UserDTO from(User user, List<UserBadgeDTO> badges) {
        return new UserDTO(
                user.id,
                user.username,
                user.name,
                user.bio,
                user.avatarUrl,
                user.email,
                badges,
                user.emailVerified,
                user.pendingEmail,
                user.twoFactorEnabled,
                user.createdAt,
                null,
                null
        );
    }

    public static UserDTO from(User user) {
        return from(user, List.of());
    }

    public static UserDTO fromProfile(User user, List<UserBadgeDTO> badges, UserStatsDTO stats, Boolean isFollowing) {
        return new UserDTO(
                user.id,
                user.username,
                user.name,
                user.bio,
                user.avatarUrl,
                user.email,
                badges,
                user.emailVerified,
                user.pendingEmail,
                user.twoFactorEnabled,
                user.createdAt,
                stats,
                isFollowing
        );
    }
}
