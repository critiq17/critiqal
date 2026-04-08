package org.acme.api.dtos;

import org.acme.domain.user.User;

import java.time.Instant;

public record UserDTO(
        Long id,
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