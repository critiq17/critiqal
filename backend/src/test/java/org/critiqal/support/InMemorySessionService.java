package org.critiqal.support;

import org.critiqal.domain.auth.session.SessionService;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySessionService implements SessionService {

    private final Map<String, UUID> store = new ConcurrentHashMap<>();

    @Override
    public String create(UUID userId) {
        var id = UUID.randomUUID().toString();
        store.put(id, userId);
        return id;
    }

    @Override
    public Optional<UUID> resolve(String sid) {
        return Optional.ofNullable(store.get(sid));
    }

    @Override
    public void destroy(String sid) {
        store.remove(sid);
    }
}
