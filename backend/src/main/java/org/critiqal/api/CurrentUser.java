package org.critiqal.api;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import org.critiqal.domain.shared.exception.ForbiddenException;

@RequestScoped
public class CurrentUser {

    private final SecurityIdentity identity;

    public CurrentUser(SecurityIdentity identity) {
        this.identity = identity;
    }

    public Long id() {
        var userId = idOrNull();
        if (userId == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userId;
    }

    public Long idOrNull() {
        if (identity.isAnonymous()) {
            return null;
        }

        var principal = identity.getPrincipal();
        return parseUserId(principal != null ? principal.getName() : null);
    }

    private Long parseUserId(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            throw new IllegalArgumentException("Authenticated user id is missing");
        }

        try {
            return Long.parseLong(principalName);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Authenticated user id is invalid", e);
        }
    }
}
