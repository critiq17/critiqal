package org.critiqal.api.event.response;

import org.critiqal.domain.event.EventAttendance;

public record AttendeeDTO(String userId, String username, String name, String avatarUrl, String status) {

    public static AttendeeDTO from(EventAttendance a) {
        var u = a.user;
        return new AttendeeDTO(u.id.toString(), u.username, u.name, u.avatarUrl, a.status.name());
    }
}
