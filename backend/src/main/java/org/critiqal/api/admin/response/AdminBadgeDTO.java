package org.critiqal.api.admin.response;

import org.critiqal.domain.badge.Badge;

// A grantable badge definition for the admin grant selector.
public record AdminBadgeDTO(
        String code,
        String name,
        String description,
        String iconUrl
) {
    public static AdminBadgeDTO from(Badge badge) {
        return new AdminBadgeDTO(badge.code, badge.name, badge.description, badge.iconUrl);
    }
}
