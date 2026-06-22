package org.critiqal.api.event.response;

import org.critiqal.domain.user.User;

public record EventHostDTO(String id, String username, String name, String avatarUrl) {

    public static EventHostDTO from(User u) {
        return new EventHostDTO(u.id.toString(), u.username, u.name, u.avatarUrl);
    }
}
