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
        Instant createdAt
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
                user.createdAt
        );
    }
    public static UserDTO from(User user) {
        return from(user, List.of());
    }
}
