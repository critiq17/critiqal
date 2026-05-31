package org.critiqal.infra.auth.ban;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.critiqal.domain.ban.UserBan;
import org.critiqal.domain.ban.repository.UserBanRepository;
import org.critiqal.domain.ban.service.BanService;

import java.util.UUID;

@ApplicationScoped
public class BanCache {

    private static final String PREFIX = "ban:status";
    private static final long TTL_SECONDS = 60;

    private final ValueCommands<String, String> redis;
    private final UserBanRepository banRepo;

    public BanCache(RedisDataSource ds, UserBanRepository banRepo) {
        this.redis = ds.value(String.class);
        this.banRepo = banRepo;
    }

    public boolean isBanned(UUID userId) {
        var key = PREFIX + userId;
        var cached = redis.get(key);
        if (cached != null) return "1".equals(cached);

        boolean banned = banRepo.findActiveByUserId(userId)
                        .map(UserBan::isActive)
                                .orElse(false);
        redis.setex(key, TTL_SECONDS, banned ? "1" : "0");
        return banned;
    }

    public void invalidate(UUID userId) {
        redis.getdel(PREFIX + userId);
    }
}
