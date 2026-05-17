package org.critiqal.api;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import org.critiqal.domain.shared.exception.ForbiddenException;

import java.util.UUID;

@RequestScoped
public class CurrentUser {

    private final SecurityIdentity identity;

    public CurrentUser(SecurityIdentity identity) {
        this.identity = identity;
    }

    public UUID id() {
        var userId = idOrNull();
        if (userId == null) {
            throw new ForbiddenException("Authentication required");
        }
        return userId;
    }

    public UUID idOrNull() {
        if (identity.isAnonymous()) {
            return null;
        }

        var principal = identity.getPrincipal();
        return parseUserId(principal != null ? principal.getName() : null);
    }

    private UUID parseUserId(String principalName) {
        if (principalName == null || principalName.isBlank()) {
            throw new IllegalArgumentException("Authenticated user id is missing");
        }

        try {
            return UUID.fromString(principalName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Authenticated user id is invalid", e);
        }
    }
}
