package org.critiqal.api.user.response;

import org.critiqal.domain.user.User;

import java.time.Instant;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String username,
        String name,
        String bio,
        String avatarUrl,
        Instant createdAt
) {
    public static UserDTO from(User user) {
        return new UserDTO(
                user.id,
                user.username,
                user.name,
                user.bio,
                user.avatarUrl,
                user.createdAt
        );
    }
}
