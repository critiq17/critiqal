package org.critiqal.domain.activity.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.critiqal.domain.activity.UserActivityStats;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserActivityStatsRepositoryImpl
        implements UserActivityStatsRepository, PanacheRepository<UserActivityStats> {

    @Override
    public UserActivityStats findOrCreate(UUID userId) {
        return find("userId", userId).firstResultOptional()
                .orElseGet(() -> {
                    var stats = new UserActivityStats();
                    stats.userId = userId;
                    persist(stats);
                    return stats;
                });
    }

    @Override
    public Optional<UserActivityStats> findByUserId(UUID userId) {
        return find("userId", userId).firstResultOptional();
    }

    @Override
    @Transactional
    public void incrementPosts(UUID userId) {
        nativeIncrement(userId, "posts_count");
    }

    @Override
    @Transactional
    public void incrementComments(UUID userId) {
        nativeIncrement(userId, "comments_count");
    }

    @Override
    @Transactional
    public void incrementLikes(UUID userId, int delta) {
        if (delta > 0) {
            nativeIncrement(userId, "likes_count");
        } else {
            getEntityManager().createNativeQuery("""
                    INSERT INTO user_activity_stats (user_id, likes_count, last_updated)
                    VALUES (?1, 0, CURRENT_TIMESTAMP)
                    ON CONFLICT (user_id) DO UPDATE
                        SET likes_count = GREATEST(user_activity_stats.likes_count - 1, 0),
                            last_updated = CURRENT_TIMESTAMP
                    """)
                    .setParameter(1, userId)
                    .executeUpdate();
        }
    }

    @Override
    @Transactional
    public void incrementEventsHosted(UUID userId) {
        nativeIncrement(userId, "events_hosted");
    }

    @Override
    @Transactional
    public void incrementEventsAttended(UUID userId) {
        nativeIncrement(userId, "events_attended");
    }

    @Override
    @Transactional
    public int incrementAllMemberDays() {
        return getEntityManager().createNativeQuery("""
                UPDATE user_activity_stats
                SET member_days = member_days + 1,
                    last_updated = CURRENT_TIMESTAMP
                """)
                .executeUpdate();
    }

    @Override
    public List<UUID> findUserIdsWithMemberDays(int days) {
        return getEntityManager()
                .createNativeQuery(
                        "SELECT user_id FROM user_activity_stats WHERE member_days = ?1",
                        UUID.class)
                .setParameter(1, days)
                .getResultList();
    }

    private void nativeIncrement(UUID userId, String columnName) {
        getEntityManager().createNativeQuery("""
                INSERT INTO user_activity_stats (user_id, %1$s, last_updated)
                VALUES (?1, 1, CURRENT_TIMESTAMP)
                ON CONFLICT (user_id) DO UPDATE
                    SET %1$s = user_activity_stats.%1$s + 1,
                        last_updated = CURRENT_TIMESTAMP
                """.formatted(columnName))
                .setParameter(1, userId)
                .executeUpdate();
    }
}
