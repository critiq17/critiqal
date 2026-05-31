package org.critiqal.infra.auth.admin;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.Config;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AdminAllowList {

    private final Set<UUID> adminIds;

    public AdminAllowList(Config config) {
        var raw = config.getOptionalValue("admin.user-ids", String.class).orElse("");
        this.adminIds = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(UUID::fromString)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean contains(UUID userId) {
        return userId != null && adminIds.contains(userId);
    }
}
