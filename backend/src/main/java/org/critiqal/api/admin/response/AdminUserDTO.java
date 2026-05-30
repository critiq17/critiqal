package org.critiqal.api.admin.response;

import org.critiqal.api.badge.response.UserBadgeDTO;
import org.critiqal.domain.user.User;

import java.util.List;
import java.util.UUID;

// Lightweight user row for the admin dashboard: identity + current badges only.
public record AdminUserDTO(
        UUID id,
        String username,
        String name,
        String avatarUrl,
        List<UserBadgeDTO> badges
) {
    public static AdminUserDTO from(User user, List<UserBadgeDTO> badges) {
        return new AdminUserDTO(user.id, user.username, user.name, user.avatarUrl, badges);
    }
}
